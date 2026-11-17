package br.com.cpet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

/** UC07: relatório agregado em Excel, PDF e JSON. Públicos e sem protocolo, coordenada ou contato. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "cpet.upload.pasta=target/uploads-teste")
class RelatorioFormatosTest {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    private MockHttpServletResponse baixar(String ext, String tipo) throws Exception {
        MockHttpServletResponse r = mvc.perform(get("/api/dados/relatorio." + ext)).andExpect(status().isOk()).andReturn()
                .getResponse();
        assertThat(r.getContentType()).startsWith(tipo);
        assertThat(r.getHeader("Content-Disposition"))
                .isEqualTo("attachment; filename=\"cpet-relatorio-agregado." + ext + "\"");
        return r;
    }

    /** Cria um caso com contato e coordenada reconhecíveis, para provar que nada disso vaza. */
    private String novoCasoComDadosPessoais() throws Exception {
        var req = multipart("/api/reportes");
        req.param("tipo", "FERIDO").param("qtdAnimais", "1").param("lat", "-23.5871").param("lng", "-46.6913")
                .param("contato", "segredo.pessoal@exemplo.org");
        return json.readTree(mvc.perform(req).andExpect(status().isCreated()).andReturn().getResponse()
                .getContentAsString()).get("codigo").asText();
    }

    @Test
    void excelTemCabecalhosCorretosEComecaComPk() throws Exception {
        byte[] b = baixar("xlsx", XLSX).getContentAsByteArray();
        assertThat(new String(b, 0, 2, StandardCharsets.ISO_8859_1)).isEqualTo("PK");
        assertThat(texto(b)).contains("atendidos_ou_adotados").contains("Dados simulados para demonstração");
    }

    @Test
    void pdfComecaComPdfETemTituloENota() throws Exception {
        byte[] b = baixar("pdf", "application/pdf").getContentAsByteArray();
        assertThat(new String(b, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
        String t = textoPdf(b);
        assertThat(t).contains("Relatório agregado de densidade (UC07)").contains("Dados simulados para demonstração");
    }

    @Test
    void jsonTemMetadadosELinhas() throws Exception {
        JsonNode j = json.readTree(baixar("json", "application/json").getContentAsString(StandardCharsets.UTF_8));
        assertThat(j.has("metadados")).isTrue();
        assertThat(j.at("/metadados/titulo").asText()).isEqualTo("Relatório agregado de densidade (UC07)");
        assertThat(j.at("/metadados/aviso").asText()).isEqualTo("Dados simulados para demonstração");
        assertThat(j.at("/metadados/baseDeCalculo").asText()).isNotBlank();
        assertThat(j.at("/metadados/geradoEm").asText()).isNotBlank();
        assertThat(j.get("linhas").size()).isGreaterThan(5);
        assertThat(j.at("/linhas/0").fieldNames()).toIterable().containsExactly("bairro", "tipo", "abertos",
                "atendidos_ou_adotados", "total", "base_de_calculo", "data_de_geracao");
    }

    @Test
    void nenhumFormatoTemProtocoloCoordenadaOuContato() throws Exception {
        String codigo = novoCasoComDadosPessoais();
        String[] saidas = {
                texto(baixar("xlsx", XLSX).getContentAsByteArray()),
                textoPdf(baixar("pdf", "application/pdf").getContentAsByteArray()),
                baixar("json", "application/json").getContentAsString(StandardCharsets.UTF_8)};
        for (String s : saidas) {
            assertThat(s).doesNotContain(codigo).doesNotContain("CP-").doesNotContain("-23.58").doesNotContain("-46.69")
                    .doesNotContain("segredo.pessoal").doesNotContain("@exemplo");
        }
    }

    private static String texto(byte[] zip) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry e;
            while ((e = z.getNextEntry()) != null) {
                sb.append(new String(z.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return sb.toString();
    }

    private static String textoPdf(byte[] pdf) throws Exception {
        PdfReader r = new PdfReader(pdf);
        PdfTextExtractor x = new PdfTextExtractor(r);
        StringBuilder sb = new StringBuilder();
        for (int p = 1; p <= r.getNumberOfPages(); p++) {
            sb.append(x.getTextFromPage(p)).append('\n');
        }
        return sb.toString();
    }
}
