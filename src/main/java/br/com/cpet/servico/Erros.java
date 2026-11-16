package br.com.cpet.servico;

public final class Erros {

    private Erros() {
    }

    public static class NaoEncontrado extends RuntimeException {
        public NaoEncontrado(String mensagem) {
            super(mensagem);
        }
    }

    public static class RegraDeNegocio extends RuntimeException {
        public RegraDeNegocio(String mensagem) {
            super(mensagem);
        }
    }

    public static class TransicaoInvalida extends RegraDeNegocio {
        public TransicaoInvalida(String mensagem) {
            super(mensagem);
        }
    }

    public static class NaoAutenticado extends RuntimeException {
        public NaoAutenticado() {
            super("Entre como um usuário para continuar.");
        }
    }

    public static class AcessoNegado extends RuntimeException {
        public AcessoNegado(String mensagem) {
            super(mensagem);
        }
    }
}
