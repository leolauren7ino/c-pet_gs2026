package br.com.cpet.dominio;

import java.util.EnumSet;
import java.util.Set;

public enum StatusCaso {
    REPORTADO, TRIAGEM, ENCAMINHADO, ATENDIDO, ADOTADO, ARQUIVADO;

    public static final Set<StatusCaso> ABERTOS = EnumSet.of(REPORTADO, TRIAGEM, ENCAMINHADO);

    /** Desfecho positivo do caso: atendido ou adotado. */
    public boolean desfecho() {
        return this == ATENDIDO || this == ADOTADO;
    }

    public boolean aberto() {
        return ABERTOS.contains(this);
    }

    public Set<StatusCaso> destinos() {
        return switch (this) {
            case REPORTADO -> EnumSet.of(TRIAGEM, ARQUIVADO);
            case TRIAGEM -> EnumSet.of(ENCAMINHADO, ARQUIVADO);
            case ENCAMINHADO -> EnumSet.of(ATENDIDO, ADOTADO, TRIAGEM, ARQUIVADO);
            case ATENDIDO, ADOTADO, ARQUIVADO -> EnumSet.noneOf(StatusCaso.class);
        };
    }

    public boolean podeIrPara(StatusCaso novo) {
        return destinos().contains(novo);
    }
}
