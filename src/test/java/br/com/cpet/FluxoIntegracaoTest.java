package br.com.cpet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.cpet.servico.BancoService;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "cpet.upload.pasta=target/uploads-teste")
class FluxoIntegracaoTest {

    private static final long OPERADOR = 1L;
    private static final long AGENTE_ONG = 3L;
    private static final long RECURSO_ONG = 4L;

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    BancoService banco;

    @BeforeEach
    void recriarSeed() {
        banco.recriarSeed();
    }

    @Test
    void reportarPriorizarEncaminharAtenderEAcompanhar() throws Exception {
        MockHttpSession operador = entrar(OPERADOR);
        MockHttpSession agente = entrar(AGENTE_ONG);

        JsonNode reporte = enviarReporte(-23.5675, -46.6905, "FERIDO", foto());
        String codigo = reporte.get("codigo").asText();
        assertThat(codigo).matches("CP-[A-Z0-9]{6}");
        assertThat(reporte.get("duplicata").asBoolean()).isFalse();

        JsonNode segundo = enviarReporte(-23.56752, -46.69052, "FERIDO", null);
        assertThat(segundo.get("duplicata").asBoolean()).isTrue();
        assertThat(segundo.get("codigo").asText()).isEqualTo(codigo);

        JsonNode fila = ler(get("/api/casos").session(operador));
        JsonNode caso = encontrarPorCodigo(fila, codigo);
        assertThat(caso.get("status").asText()).isEqualTo("REPORTADO");
        assertThat(caso.get("score").asInt()).isGreaterThan(40);
        assertThat(caso.get("reportes").asInt()).isEqualTo(2);
        assertThat(pontuacoes(fila)).isSortedAccordingTo((a, b) -> b - a);

        long id = caso.get("id").asLong();
        JsonNode detalhe = ler(get("/api/casos/" + id).session(operador));
        assertThat(detalhe.at("/score/fatores")).hasSize(4);
        assertThat(detalhe.get("sugestoes")).hasSize(3);
        assertThat(detalhe.at("/reportes/0/fotoUrl").asText()).startsWith("/api/fotos/");
        mvc.perform(get(detalhe.at("/reportes/0/fotoUrl").asText()).session(operador)).andExpect(status().isOk());
        mvc.perform(get(detalhe.at("/reportes/0/fotoUrl").asText())).andExpect(status().isUnauthorized());

        int vagasAntes = ler(get("/api/recurso").session(agente)).get("vagas").asInt();
        mvc.perform(post("/api/casos/" + id + "/encaminhar").session(operador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recursoId\":" + RECURSO_ONG + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENCAMINHADO"));
        assertThat(ler(get("/api/recurso").session(agente)).get("vagas").asInt()).isEqualTo(vagasAntes - 1);

        JsonNode daOng = ler(get("/api/recurso/casos").session(agente));
        assertThat(daOng).anyMatch(c -> c.at("/caso/id").asLong() == id);

        mvc.perform(post("/api/recurso/casos/" + id + "/aceitar").session(agente)).andExpect(status().isOk());
        mvc.perform(post("/api/recurso/casos/" + id + "/atender").session(agente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observacao\":\"Atendido e vacinado.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATENDIDO"));
        assertThat(ler(get("/api/recurso").session(agente)).get("vagas").asInt()).isEqualTo(vagasAntes);

        MvcResult publico = mvc.perform(get("/api/acompanhar/" + codigo.toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATENDIDO"))
                .andReturn();
        JsonNode acompanhamento = json.readTree(publico.getResponse().getContentAsString());
        assertThat(acompanhamento.get("etapas")).extracting(e -> e.get("status").asText())
                .containsExactly("REPORTADO", "TRIAGEM", "ENCAMINHADO", "ATENDIDO");
        assertThat(publico.getResponse().getContentAsString()).doesNotContain("\"lat\"", "\"lng\"", "contato");
    }

    @Test
    void recusaDevolveVagaEVoltaParaTriagem() throws Exception {
        MockHttpSession operador = entrar(OPERADOR);
        MockHttpSession agente = entrar(AGENTE_ONG);
        String codigo = enviarReporte(-23.5700, -46.6800, "FILHOTE", null).get("codigo").asText();
        long id = encontrarPorCodigo(ler(get("/api/casos").session(operador)), codigo).get("id").asLong();
        int vagasAntes = ler(get("/api/recurso").session(agente)).get("vagas").asInt();

        mvc.perform(post("/api/casos/" + id + "/encaminhar").session(operador)
                .contentType(MediaType.APPLICATION_JSON).content("{\"recursoId\":" + RECURSO_ONG + "}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/recurso/casos/" + id + "/recusar").session(agente)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"observacao\":\"Sem espaço hoje.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TRIAGEM"));

        assertThat(ler(get("/api/recurso").session(agente)).get("vagas").asInt()).isEqualTo(vagasAntes);
        JsonNode historico = ler(get("/api/casos/" + id).session(operador)).get("historico");
        assertThat(historico).extracting(h -> h.get("novo").asText())
                .containsExactly("REPORTADO", "TRIAGEM", "ENCAMINHADO", "TRIAGEM");
    }

    @Test
    void transicaoInvalidaEhRecusada() throws Exception {
        MockHttpSession operador = entrar(OPERADOR);
        MockHttpSession agente = entrar(AGENTE_ONG);
        String codigo = enviarReporte(-23.5710, -46.6810, "DOENTE", null).get("codigo").asText();
        long id = encontrarPorCodigo(ler(get("/api/casos").session(operador)), codigo).get("id").asLong();

        mvc.perform(post("/api/casos/" + id + "/arquivar").session(operador)
                .contentType(MediaType.APPLICATION_JSON).content("{\"observacao\":\"Duplicado.\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/casos/" + id + "/encaminhar").session(operador)
                .contentType(MediaType.APPLICATION_JSON).content("{\"recursoId\":" + RECURSO_ONG + "}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/recurso/casos/" + id + "/aceitar").session(agente))
                .andExpect(status().isForbidden());
    }

    @Test
    void areaInternaExigeLoginEPerfil() throws Exception {
        mvc.perform(get("/api/casos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/alertas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/casos").session(entrar(AGENTE_ONG))).andExpect(status().isForbidden());
        mvc.perform(get("/api/recurso/casos").session(entrar(OPERADOR))).andExpect(status().isForbidden());
    }

    @Test
    void dadosPublicosSoTrazemCelulasArredondadas() throws Exception {
        JsonNode celulas = ler(get("/api/dados/celulas"));

        assertThat(celulas).isNotEmpty();
        for (JsonNode c : celulas) {
            assertThat(c.get("lat").asDouble()).isEqualTo(Math.round(c.get("lat").asDouble() * 100) / 100.0);
            assertThat(c.get("lng").asDouble()).isEqualTo(Math.round(c.get("lng").asDouble() * 100) / 100.0);
            assertThat(c.has("codigo")).isFalse();
            long somaPorTipo = 0;
            for (JsonNode n : c.get("abertosPorTipo")) {
                somaPorTipo += n.asLong();
            }
            assertThat(somaPorTipo).isEqualTo(c.get("abertos").asLong());
        }
    }

    @Test
    void alertasTrazemCelulasComCincoOuMaisCasosAbertos() throws Exception {
        JsonNode alertas = ler(get("/api/alertas").session(entrar(OPERADOR)));

        assertThat(alertas.get("celulasCriticas")).isNotEmpty()
                .allSatisfy(c -> assertThat(c.get("abertos").asInt()).isGreaterThanOrEqualTo(5));
        assertThat(alertas.get("casosAtualizados")).isNotEmpty();
    }

    @Test
    void rejeitaArquivoQueNaoEhImagem() throws Exception {
        MockMultipartFile falso = new MockMultipartFile("foto", "x.jpg", "image/jpeg", "nao sou imagem".getBytes());

        mvc.perform(multipart("/api/reportes").file(falso)
                        .param("tipo", "FERIDO").param("qtdAnimais", "1")
                        .param("lat", "-23.5").param("lng", "-46.6"))
                .andExpect(status().isUnprocessableEntity());
    }

    private JsonNode enviarReporte(double lat, double lng, String tipo, MockMultipartFile foto) throws Exception {
        var req = multipart("/api/reportes");
        req.param("tipo", tipo).param("qtdAnimais", "1")
                .param("lat", String.valueOf(lat)).param("lng", String.valueOf(lng))
                .param("descricao", "Animal na calçada.").param("contato", "morador@exemplo.com");
        if (foto != null) {
            req.file(foto);
        }
        MvcResult r = mvc.perform(req).andExpect(status().isCreated()).andReturn();
        return json.readTree(r.getResponse().getContentAsString());
    }

    private MockHttpSession entrar(long usuarioId) throws Exception {
        MockHttpSession sessao = new MockHttpSession();
        mvc.perform(post("/api/sessao").session(sessao).contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioId\":" + usuarioId + "}")).andExpect(status().isOk());
        return sessao;
    }

    private JsonNode ler(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req) throws Exception {
        MvcResult r = mvc.perform(req).andExpect(status().isOk()).andReturn();
        return json.readTree(r.getResponse().getContentAsString());
    }

    private static JsonNode encontrarPorCodigo(JsonNode lista, String codigo) {
        for (JsonNode c : lista) {
            if (codigo.equals(c.get("codigo").asText())) {
                return c;
            }
        }
        throw new AssertionError("Caso " + codigo + " não está na fila");
    }

    private static List<Integer> pontuacoes(JsonNode fila) {
        return fila.findValues("score").stream().map(JsonNode::asInt).toList();
    }

    private static MockMultipartFile foto() throws Exception {
        BufferedImage img = new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(img, "png", saida);
        return new MockMultipartFile("foto", "gato.png", "image/png", saida.toByteArray());
    }
}
