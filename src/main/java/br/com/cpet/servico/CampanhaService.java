package br.com.cpet.servico;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.cpet.dominio.Campanha;
import br.com.cpet.dominio.Repositorios.CampanhaRepository;
import br.com.cpet.servico.Visoes.CampanhaVisao;

/** Campanha mínima de arrecadação por caso. As contribuições são simuladas: nenhum pagamento acontece. */
@Service
@Transactional
public class CampanhaService {

    static final BigDecimal META_MAXIMA = new BigDecimal("1000000");
    static final BigDecimal CONTRIBUICAO_MAXIMA = new BigDecimal("10000");

    private final CampanhaRepository campanhas;
    private final Clock relogio;

    public CampanhaService(CampanhaRepository campanhas, Clock relogio) {
        this.campanhas = campanhas;
        this.relogio = relogio;
    }

    public CampanhaVisao criar(Long casoId, BigDecimal meta) {
        if (meta == null || meta.compareTo(BigDecimal.ZERO) <= 0 || meta.compareTo(META_MAXIMA) > 0) {
            throw new Erros.RegraDeNegocio("Informe uma meta maior que zero e de até R$ 1.000.000.");
        }
        if (campanhas.findByCasoIdOrderByIdDesc(casoId).stream().anyMatch(c -> c.getAtiva() == 1)) {
            throw new Erros.RegraDeNegocio("Este caso já tem uma campanha ativa.");
        }
        Campanha c = new Campanha();
        c.setCasoId(casoId);
        c.setMeta(meta.setScale(2, RoundingMode.HALF_UP));
        c.setValorArrecadado(BigDecimal.ZERO.setScale(2));
        c.setAtiva(1);
        c.setCriadoEm(LocalDateTime.now(relogio));
        return visao(campanhas.save(c));
    }

    public CampanhaVisao contribuir(Long campanhaId, BigDecimal valor) {
        Campanha c = campanhas.findById(campanhaId)
                .orElseThrow(() -> new Erros.NaoEncontrado("Campanha não encontrada."));
        if (valor == null || valor.setScale(2, RoundingMode.HALF_UP).compareTo(BigDecimal.ZERO) <= 0) {
            throw new Erros.RegraDeNegocio("Informe um valor maior que zero.");
        }
        if (valor.compareTo(CONTRIBUICAO_MAXIMA) > 0) {
            throw new Erros.RegraDeNegocio("O valor simulado vai até R$ 10.000 por contribuição.");
        }
        if (c.getAtiva() != 1) {
            throw new Erros.RegraDeNegocio("Esta campanha já atingiu a meta ou foi encerrada.");
        }
        c.setValorArrecadado(c.getValorArrecadado().add(valor.setScale(2, RoundingMode.HALF_UP)));
        if (c.getValorArrecadado().compareTo(c.getMeta()) >= 0) {
            c.setAtiva(0);
        }
        return visao(c);
    }

    @Transactional(readOnly = true)
    public Optional<CampanhaVisao> doCaso(Long casoId) {
        return campanhas.findByCasoIdOrderByIdDesc(casoId).stream().findFirst().map(CampanhaService::visao);
    }

    private static CampanhaVisao visao(Campanha c) {
        return new CampanhaVisao(c.getId(), c.getMeta(), c.getValorArrecadado(), c.getAtiva() == 1);
    }
}
