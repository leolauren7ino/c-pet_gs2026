package br.com.cpet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.cpet.servico.BancoService;

/**
 * Garante o argumento de privacidade: o que é público nunca traz coordenada exata,
 * contato, descrição, observação interna, nome de agente ou caminho de foto.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "cpet.upload.pasta=target/uploads-teste")
class PrivacidadeTest {

    private static final String CONTATO = "contato-reservado@exemplo.com";
    private static final String DESCRICAO = "descricao-reservada-xyz";
    private static final double LAT = -23.57123;
    private static final double LNG = -46.64987;
    private static final Set<String> CHAVES_PROIBIDAS = Set.of(
            "contato", "contatoOpcional", "contato_opcional", "fotoUrl", "fotoPath", "foto_path", "foto",
            "descricao", "observacao", "autor", "reportes", "recurso", "recursoId", "historico");

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
    void endpointsPublicosNaoVazamCoordenadaExataContatoDescricaoNemFoto() throws Exception {
        MockHttpSession operador = entrar(1L);
        String codigoNovo = reportarComFoto();
        JsonNode fila = ler(get("/api/casos").session(operador));

        List<String> segredos = new ArrayList<>(List.of(CONTATO, DESCRICAO, "Carlos Almeida", "Juliana Rocha",
                "Paulo Mendes", "Clínica Veterinária Jardim", "Associação Abrigo Central", "seed-fotos", "/api/fotos", ".png"));
        for (JsonNode caso : fila) {
            segredos.add(caso.get("lat").asText());
            segredos.add(caso.get("lng").asText());
        }
        long idNovo = 0;
        for (JsonNode caso : fila) {
            if (codigoNovo.equals(caso.get("codigo").asText())) {
                idNovo = caso.get("id").asLong();
            }
        }
        String arquivoDaFoto = ler(get("/api/casos/" + idNovo).session(operador)).at("/reportes/0/fotoUrl").asText();
        segredos.add(arquivoDaFoto.substring(arquivoDaFoto.lastIndexOf('/') + 1));

        List<String> urlsPublicas = new ArrayList<>(List.of("/api/dados/celulas", "/api/dados/bairros",
                "/api/dados/resumo", "/api/info", "/api/acompanhar/" + codigoNovo));
        for (JsonNode caso : fila) {
            urlsPublicas.add("/api/acompanhar/" + caso.get("codigo").asText());
        }

        for (String url : urlsPublicas) {
            String corpo = publico(url);
            for (String segredo : segredos) {
                assertThat(corpo).as("%s não pode conter %s", url, segredo).doesNotContain(segredo);
            }
            verificarJson(url, json.readTree(corpo));
        }
    }

    @Test
    void respostaDoReporteSoTrazCodigoEIndicadorDeDuplicata() throws Exception {
        MvcResult r = mvc.perform(multipart("/api/reportes")
                        .param("tipo", "DOENTE").param("qtdAnimais", "1")
                        .param("lat", String.valueOf(LAT)).param("lng", String.valueOf(LNG))
                        .param("descricao", DESCRICAO).param("contato", CONTATO))
                .andExpect(status().isCreated()).andReturn();

        JsonNode corpo = json.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertThat(corpo.fieldNames()).toIterable().containsExactlyInAnyOrder("codigo", "duplicata");
    }

    @Test
    void fotoEnviadaPorCidadaoSoSaiParaUsuarioAutenticado() throws Exception {
        MockHttpSession operador = entrar(1L);
        String codigo = reportarComFoto();
        long id = 0;
        for (JsonNode caso : ler(get("/api/casos").session(operador))) {
            if (codigo.equals(caso.get("codigo").asText())) {
                id = caso.get("id").asLong();
            }
        }
        String url = ler(get("/api/casos/" + id).session(operador)).at("/reportes/0/fotoUrl").asText();

        mvc.perform(get(url)).andExpect(status().isUnauthorized());
        mvc.perform(get(url).session(operador)).andExpect(status().isOk());
        mvc.perform(get("/api/fotos/..%2F..%2Fapplication.yml").session(operador)).andExpect(status().is4xxClientError());
    }

    @Test
    void agenteNaoLeCasoDeOutroRecurso() throws Exception {
        MockHttpSession agenteClinica = entrar(2L);
        MockHttpSession agenteOng = entrar(3L);
        long casoDaClinica = 0;
        for (JsonNode d : ler(get("/api/recurso/casos").session(agenteClinica))) {
            casoDaClinica = d.at("/caso/id").asLong();
        }
        assertThat(casoDaClinica).isPositive();

        final long idDaClinica = casoDaClinica;
        assertThat(ler(get("/api/recurso/casos").session(agenteOng)))
                .noneMatch(d -> d.at("/caso/id").asLong() == idDaClinica);
        mvc.perform(post("/api/recurso/casos/" + casoDaClinica + "/aceitar").session(agenteOng))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/casos/" + casoDaClinica).session(agenteOng)).andExpect(status().isForbidden());
    }

    private void verificarJson(String url, JsonNode no) {
        if (no.isObject()) {
            no.fields().forEachRemaining(e -> {
                assertThat(CHAVES_PROIBIDAS).as("%s expõe o campo %s", url, e.getKey()).doesNotContain(e.getKey());
                if ((e.getKey().equals("lat") || e.getKey().equals("lng")) && e.getValue().isNumber()) {
                    BigDecimal v = e.getValue().decimalValue().stripTrailingZeros();
                    assertThat(Math.max(v.scale(), 0)).as("%s: %s com mais de 2 casas", url, e.getKey()).isLessThanOrEqualTo(2);
                }
                verificarJson(url, e.getValue());
            });
        } else if (no.isArray()) {
            no.forEach(item -> verificarJson(url, item));
        }
    }

    private String publico(String url) throws Exception {
        MvcResult r = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
        return r.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String reportarComFoto() throws Exception {
        BufferedImage img = new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(img, "png", saida);
        MockMultipartFile foto = new MockMultipartFile("foto", "gato.png", "image/png", saida.toByteArray());
        var req = multipart("/api/reportes");
        req.file(foto).param("tipo", "FERIDO").param("qtdAnimais", "1")
                .param("lat", String.valueOf(LAT)).param("lng", String.valueOf(LNG))
                .param("descricao", DESCRICAO).param("contato", CONTATO);
        MvcResult r = mvc.perform(req).andExpect(status().isCreated()).andReturn();
        return json.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("codigo").asText();
    }

    private MockHttpSession entrar(long usuarioId) throws Exception {
        MockHttpSession sessao = new MockHttpSession();
        mvc.perform(post("/api/sessao").session(sessao).contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioId\":" + usuarioId + "}")).andExpect(status().isOk());
        return sessao;
    }

    private JsonNode ler(MockHttpServletRequestBuilder req) throws Exception {
        MvcResult r = mvc.perform(req).andExpect(status().isOk()).andReturn();
        return json.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
