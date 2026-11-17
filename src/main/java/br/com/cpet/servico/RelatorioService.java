package br.com.cpet.servico;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Relatório agregado (UC07) em Excel, PDF e JSON. Parte do mesmo CSV público, então as colunas e os números são
 * sempre os mesmos e nenhum formato enxerga protocolo, coordenada ou contato.
 */
@Service
public class RelatorioService {

    public static final String TITULO = "Relatório agregado de densidade (UC07)";
    public static final String AVISO = "Dados simulados para demonstração";
    public static final String[] COLUNAS = {"bairro", "tipo", "abertos", "atendidos_ou_adotados", "total",
            "base_de_calculo", "data_de_geracao"};

    public record Linha(@JsonProperty("bairro") String bairro, @JsonProperty("tipo") String tipo,
                        @JsonProperty("abertos") long abertos,
                        @JsonProperty("atendidos_ou_adotados") long atendidosOuAdotados,
                        @JsonProperty("total") long total, @JsonProperty("base_de_calculo") String baseDeCalculo,
                        @JsonProperty("data_de_geracao") String dataDeGeracao) {
    }

    public record Relatorio(Map<String, String> metadados, List<Linha> linhas) {
    }

    private final DadosAbertosService dados;

    public RelatorioService(DadosAbertosService dados) {
        this.dados = dados;
    }

    public Relatorio relatorio() {
        String[] linhasCsv = dados.relatorioCsv().split("\\r?\\n");
        List<Linha> linhas = new ArrayList<>();
        for (int i = 1; i < linhasCsv.length; i++) {
            if (linhasCsv[i].isBlank()) {
                continue;
            }
            String[] c = linhasCsv[i].split(";", -1);
            linhas.add(new Linha(c[0], c[1], Long.parseLong(c[2]), Long.parseLong(c[3]), Long.parseLong(c[4]), c[5], c[6]));
        }
        String base = linhas.isEmpty() ? "todos os casos registrados" : linhas.get(0).baseDeCalculo();
        String data = linhas.isEmpty()
                ? java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                : linhas.get(0).dataDeGeracao();
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("titulo", TITULO);
        meta.put("geradoEm", data);
        meta.put("baseDeCalculo", base);
        meta.put("aviso", AVISO);
        return new Relatorio(meta, linhas);
    }

    public byte[] xlsx() {
        Relatorio r = relatorio();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Workbook wb = new Workbook(out, "C-PET", "1.0");
            Worksheet ws = wb.newWorksheet("Relatório agregado");
            for (int c = 0; c < COLUNAS.length; c++) {
                ws.value(0, c, COLUNAS[c]);
                ws.style(0, c).bold().fillColor("3CC3A4").fontColor("0F0F0E").set();
            }
            int l = 1;
            for (Linha x : r.linhas()) {
                ws.value(l, 0, x.bairro());
                ws.value(l, 1, x.tipo());
                ws.value(l, 2, x.abertos());
                ws.value(l, 3, x.atendidosOuAdotados());
                ws.value(l, 4, x.total());
                ws.value(l, 5, x.baseDeCalculo());
                ws.value(l, 6, x.dataDeGeracao());
                l++;
            }
            l++;
            ws.value(l, 0, "C-PET: " + TITULO);
            ws.style(l, 0).bold().set();
            ws.value(l + 1, 0, AVISO);
            ws.value(l + 2, 0, "Base de cálculo: " + r.metadados().get("baseDeCalculo"));
            ws.value(l + 3, 0, "Gerado em: " + r.metadados().get("geradoEm"));
            int[] larguras = {22, 14, 10, 22, 8, 28, 18};
            for (int c = 0; c < larguras.length; c++) {
                ws.width(c, larguras[c]);
            }
            ws.freezePane(0, 1);
            wb.finish();
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao gerar o Excel", e);
        }
    }

    public byte[] pdf() {
        Relatorio r = relatorio();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 50);
            PdfWriter.getInstance(doc, out);
            doc.addTitle(TITULO);
            doc.addAuthor("C-PET");
            com.lowagie.text.HeaderFooter rodape = new com.lowagie.text.HeaderFooter(new Phrase(
                    "Base de cálculo: " + r.metadados().get("baseDeCalculo") + ". Gerado em " + r.metadados().get("geradoEm")
                            + ". " + AVISO + ". Página ", new Font(Font.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY)), true);
            rodape.setAlignment(Element.ALIGN_CENTER);
            doc.setFooter(rodape);
            doc.open();

            Color fundo = new Color(0x0F, 0x0F, 0x0E);
            Color menta = new Color(0x3C, 0xC3, 0xA4);
            Font fTitulo = new Font(Font.HELVETICA, 16, Font.BOLD, menta);
            Font fBranco = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(0xEA, 0xF1, 0xEF));
            Font fCab = new Font(Font.HELVETICA, 9, Font.BOLD, fundo);
            Font fCel = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLACK);

            // Faixa escura com o emblema: o PNG tem fundo do mesmo tom, então não aparece disco.
            PdfPTable topo = new PdfPTable(new float[] {1f, 6f});
            topo.setWidthPercentage(100);
            PdfPCell img = new PdfPCell();
            img.setBackgroundColor(fundo);
            img.setBorder(Rectangle.NO_BORDER);
            img.setPadding(8);
            try (InputStream in = getClass().getResourceAsStream("/static/img/emblema.png")) {
                if (in != null) {
                    Image emblema = Image.getInstance(in.readAllBytes());
                    emblema.scaleToFit(52, 52);
                    img.addElement(emblema);
                }
            }
            topo.addCell(img);
            PdfPCell tit = new PdfPCell();
            tit.setBackgroundColor(fundo);
            tit.setBorder(Rectangle.NO_BORDER);
            tit.setPadding(8);
            tit.setVerticalAlignment(Element.ALIGN_MIDDLE);
            tit.addElement(new Paragraph("C-PET", new Font(Font.HELVETICA, 11, Font.BOLD, menta)));
            tit.addElement(new Paragraph(TITULO, fTitulo));
            tit.addElement(new Paragraph("Gerado em " + r.metadados().get("geradoEm"), fBranco));
            topo.addCell(tit);
            doc.add(topo);

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph(AVISO, new Font(Font.HELVETICA, 10, Font.BOLDITALIC)));
            doc.add(new Paragraph(" "));

            PdfPTable t = new PdfPTable(new float[] {3f, 2f, 1.2f, 1.8f, 1f});
            t.setWidthPercentage(100);
            t.setHeaderRows(1);
            for (String h : new String[] {"Bairro", "Tipo", "Abertos", "Atendidos ou adotados", "Total"}) {
                PdfPCell c = new PdfPCell(new Phrase(h, fCab));
                c.setBackgroundColor(menta);
                c.setPadding(5);
                t.addCell(c);
            }
            for (Linha x : r.linhas()) {
                for (String v : new String[] {x.bairro(), x.tipo(), "" + x.abertos(), "" + x.atendidosOuAdotados(), "" + x.total()}) {
                    PdfPCell c = new PdfPCell(new Phrase(v, fCel));
                    c.setPadding(4);
                    c.setBorderColor(Color.LIGHT_GRAY);
                    t.addCell(c);
                }
            }
            doc.add(t);

            doc.close();
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao gerar o PDF", e);
        }
    }
}
