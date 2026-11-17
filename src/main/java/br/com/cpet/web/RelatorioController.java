package br.com.cpet.web;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.cpet.servico.RelatorioService;
import br.com.cpet.servico.RelatorioService.Relatorio;

/** UC07: o relatório agregado em Excel, PDF e JSON (o CSV fica em PublicoController). Público, só agregados. */
@RestController
@RequestMapping("/api/dados")
public class RelatorioController {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final RelatorioService relatorios;

    public RelatorioController(RelatorioService relatorios) {
        this.relatorios = relatorios;
    }

    @GetMapping(value = "/relatorio.xlsx", produces = XLSX)
    public ResponseEntity<byte[]> xlsx() {
        return anexo("xlsx", MediaType.parseMediaType(XLSX)).body(relatorios.xlsx());
    }

    @GetMapping(value = "/relatorio.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf() {
        return anexo("pdf", MediaType.APPLICATION_PDF).body(relatorios.pdf());
    }

    @GetMapping(value = "/relatorio.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Relatorio> json() {
        return anexo("json", MediaType.APPLICATION_JSON).body(relatorios.relatorio());
    }

    private static ResponseEntity.BodyBuilder anexo(String extensao, MediaType tipo) {
        return ResponseEntity.ok().contentType(tipo)
                .header("Content-Disposition", "attachment; filename=\"cpet-relatorio-agregado." + extensao + "\"");
    }
}
