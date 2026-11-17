package br.com.cpet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.cpet.servico.BancoService;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "cpet.upload.pasta=target/uploads-teste")
class CampanhaTest {

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
    void agentePedeRecursoExtraECidadaoContribuiComSoma() throws Exception {
        MockHttpSession operador = entrar(1);
        MockHttpSession agente = entrar(3);
        String codigo = novoCasoEncaminhado(operador);
        long id = idDoCaso(operador, codigo);

        JsonNode campanha = ler(post("/api/recurso/casos/" + id + "/campanha").session(agente)
                .contentType(MediaType.APPLICATION_JSON).content("{\"meta\":500}"));
        assertThat(campanha.get("meta").decimalValue()).isEqualByComparingTo("500");
        assertThat(campanha.get("valorArrecadado").decimalValue()).isEqualByComparingTo("0");

        long campanhaId = campanha.get("id").asLong();
        contribuir(campanhaId, "120.50");
        JsonNode soma = contribuir(campanhaId, "79.50");
        assertThat(soma.get("valorArrecadado").decimalValue()).isEqualByComparingTo("200");
        assertThat(soma.get("ativa").asBoolean()).isTrue();

        JsonNode publico = ler(get("/api/acompanhar/" + codigo));
        assertThat(publico.at("/campanha/id").asLong()).isEqualTo(campanhaId);
        assertThat(publico.at("/campanha/valorArrecadado").decimalValue()).isEqualByComparingTo("200");

        JsonNode fim = contribuir(campanhaId, "300");
        assertThat(fim.get("ativa").asBoolean()).isFalse();
        mvc.perform(post("/api/campanhas/" + campanhaId + "/contribuicoes")
                .contentType(MediaType.APPLICATION_JSON).content("{\"valor\":10}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void campanhaInexistenteEValoresInvalidosSaoRejeitados() throws Exception {
        mvc.perform(post("/api/campanhas/999999/contribuicoes")
                .contentType(MediaType.APPLICATION_JSON).content("{\"valor\":10}"))
                .andExpect(status().isNotFound());
        MockHttpSession operador = entrar(1);
        MockHttpSession agente = entrar(3);
        long id = idDoCaso(operador, novoCasoEncaminhado(operador));
        long campanhaId = ler(post("/api/recurso/casos/" + id + "/campanha").session(agente)
                .contentType(MediaType.APPLICATION_JSON).content("{\"meta\":100}")).get("id").asLong();
        for (String valor : new String[] {"0", "-5", "10001"}) {
            mvc.perform(post("/api/campanhas/" + campanhaId + "/contribuicoes")
                    .contentType(MediaType.APPLICATION_JSON).content("{\"valor\":" + valor + "}"))
                    .andExpect(status().isUnprocessableEntity());
        }
        mvc.perform(post("/api/recurso/casos/" + id + "/campanha").session(agente)
                .contentType(MediaType.APPLICATION_JSON).content("{\"meta\":100}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void agenteDeOutroRecursoNaoCriaCampanhaEAdocaoEDesfecho() throws Exception {
        MockHttpSession operador = entrar(1);
        MockHttpSession outro = entrar(2);
        MockHttpSession agente = entrar(3);
        String codigo = novoCasoEncaminhado(operador);
        long id = idDoCaso(operador, codigo);
        mvc.perform(post("/api/recurso/casos/" + id + "/campanha").session(outro)
                .contentType(MediaType.APPLICATION_JSON).content("{\"meta\":100}"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/recurso/casos/" + id + "/adotar").session(agente)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"observacao\":\"Adotado por uma familia.\",\"meta\":80}"))
                .andExpect(status().isOk());
        JsonNode publico = ler(get("/api/acompanhar/" + codigo));
        assertThat(publico.get("status").asText()).isEqualTo("ADOTADO");
        assertThat(publico.at("/campanha/meta").decimalValue()).isEqualByComparingTo("80");
        assertThat(ler(get("/api/dados/resumo")).get("atendidos").asInt()).isGreaterThanOrEqualTo(14);
    }

    @Test
    void relatorioCsvSoTemAgregados() throws Exception {
        String csv = mvc.perform(get("/api/dados/relatorio.csv")).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
        String[] linhas = csv.replace("﻿", "").split("\r?\n");
        assertThat(linhas[0]).isEqualTo(
                "bairro;tipo;abertos;atendidos_ou_adotados;total;base_de_calculo;data_de_geracao");
        assertThat(linhas.length).isGreaterThan(5);
        assertThat(csv).doesNotContain("CP-").doesNotContain("-23.");
    }

    @Test
    void relatorioCsvForcaDownloadEComecaComBomECabecalho() throws Exception {
        var res = mvc.perform(get("/api/dados/relatorio.csv")).andExpect(status().isOk()).andReturn().getResponse();
        assertThat(res.getHeader("Content-Disposition")).isEqualTo("attachment; filename=\"cpet-relatorio-agregado.csv\"");
        assertThat(res.getContentType()).isEqualToIgnoringCase("text/csv;charset=UTF-8");
        byte[] corpo = res.getContentAsByteArray();
        assertThat(corpo[0]).isEqualTo((byte) 0xEF);
        assertThat(corpo[1]).isEqualTo((byte) 0xBB);
        assertThat(corpo[2]).isEqualTo((byte) 0xBF);
        String primeira = new String(corpo, 3, corpo.length - 3, StandardCharsets.UTF_8).split("\\R")[0];
        assertThat(primeira).isEqualTo("bairro;tipo;abertos;atendidos_ou_adotados;total;base_de_calculo;data_de_geracao");
    }

    private JsonNode contribuir(long campanhaId, String valor) throws Exception {
        return ler(post("/api/campanhas/" + campanhaId + "/contribuicoes")
                .contentType(MediaType.APPLICATION_JSON).content("{\"valor\":" + valor + "}"));
    }

    private String novoCasoEncaminhado(MockHttpSession operador) throws Exception {
        var req = multipart("/api/reportes");
        req.param("tipo", "FERIDO").param("qtdAnimais", "1").param("lat", "-23.5675").param("lng", "-46.6905")
                .param("contato", "teste@exemplo.org");
        String codigo = json.readTree(mvc.perform(req).andExpect(status().isCreated()).andReturn().getResponse()
                .getContentAsString()).get("codigo").asText();
        long id = idDoCaso(operador, codigo);
        mvc.perform(post("/api/casos/" + id + "/encaminhar").session(operador)
                .contentType(MediaType.APPLICATION_JSON).content("{\"recursoId\":4}"))
                .andExpect(status().isOk());
        return codigo;
    }

    private long idDoCaso(MockHttpSession operador, String codigo) throws Exception {
        for (JsonNode c : ler(get("/api/casos").session(operador))) {
            if (codigo.equals(c.get("codigo").asText())) {
                return c.get("id").asLong();
            }
        }
        throw new AssertionError("caso nao encontrado");
    }

    private MockHttpSession entrar(long usuarioId) throws Exception {
        MockHttpSession sessao = new MockHttpSession();
        mvc.perform(post("/api/sessao").session(sessao).contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioId\":" + usuarioId + "}")).andExpect(status().isOk());
        return sessao;
    }

    private JsonNode ler(MockHttpServletRequestBuilder req) throws Exception {
        return json.readTree(mvc.perform(req).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8));
    }
}
