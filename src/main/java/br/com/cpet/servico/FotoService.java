package br.com.cpet.servico;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import br.com.cpet.config.UploadProperties;

@Service
public class FotoService {

    private static final Pattern NOME_VALIDO = Pattern.compile("[0-9a-f\\-]{36}\\.(jpg|png)");

    private final UploadProperties upload;

    public FotoService(UploadProperties upload) {
        this.upload = upload;
    }

    /** Valida o formato real pelos bytes, recodifica a imagem (descartando EXIF) e grava com nome UUID. */
    public String salvar(MultipartFile arquivo) {
        if (arquivo.getSize() > upload.getMaxBytes()) {
            throw new Erros.RegraDeNegocio("A foto passa de 5 MB. Escolha uma imagem menor.");
        }
        try {
            byte[] bytes = arquivo.getBytes();
            String extensao = detectarExtensao(bytes);
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(bytes));
            if (extensao == null || original == null) {
                throw new Erros.RegraDeNegocio("Envie uma foto em JPG ou PNG.");
            }
            BufferedImage limpa = extensao.equals("jpg") ? paraRgb(original) : original;
            String nome = UUID.randomUUID() + "." + extensao;
            Path pasta = Path.of(upload.getPasta()).toAbsolutePath().normalize();
            Files.createDirectories(pasta);
            ImageIO.write(limpa, extensao.equals("jpg") ? "jpg" : "png", pasta.resolve(nome).toFile());
            return nome;
        } catch (IOException e) {
            throw new Erros.RegraDeNegocio("Não foi possível processar a foto.");
        }
    }

    public Path localizar(String nome) {
        if (!NOME_VALIDO.matcher(nome).matches()) {
            throw new Erros.NaoEncontrado("Foto não encontrada.");
        }
        Path caminho = Path.of(upload.getPasta()).toAbsolutePath().normalize().resolve(nome);
        if (!Files.exists(caminho)) {
            throw new Erros.NaoEncontrado("Foto não encontrada.");
        }
        return caminho;
    }

    private static String detectarExtensao(byte[] b) {
        if (b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "png";
        }
        return null;
    }

    private static BufferedImage paraRgb(BufferedImage origem) {
        BufferedImage rgb = new BufferedImage(origem.getWidth(), origem.getHeight(), BufferedImage.TYPE_INT_RGB);
        rgb.createGraphics().drawImage(origem, 0, 0, java.awt.Color.WHITE, null);
        return rgb;
    }
}
