package br.com.cpet.web;

import java.util.Arrays;

import org.springframework.stereotype.Component;

import br.com.cpet.dominio.Perfil;
import br.com.cpet.dominio.Repositorios.UsuarioRepository;
import br.com.cpet.dominio.Usuario;
import br.com.cpet.servico.Erros;
import jakarta.servlet.http.HttpSession;

@Component
public class SessaoService {

    private static final String ATRIBUTO = "usuarioId";

    private final UsuarioRepository usuarios;

    public SessaoService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public Usuario entrar(HttpSession sessao, Long usuarioId) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new Erros.NaoEncontrado("Usuário não encontrado."));
        sessao.setAttribute(ATRIBUTO, usuario.getId());
        return usuario;
    }

    public void sair(HttpSession sessao) {
        sessao.invalidate();
    }

    public Usuario exigir(HttpSession sessao, Perfil... perfisPermitidos) {
        Object id = sessao.getAttribute(ATRIBUTO);
        if (id == null) {
            throw new Erros.NaoAutenticado();
        }
        Usuario usuario = usuarios.findById((Long) id).orElseThrow(Erros.NaoAutenticado::new);
        if (perfisPermitidos.length > 0 && !Arrays.asList(perfisPermitidos).contains(usuario.getPerfil())) {
            throw new Erros.AcessoNegado("Seu perfil não tem acesso a esta área.");
        }
        return usuario;
    }
}
