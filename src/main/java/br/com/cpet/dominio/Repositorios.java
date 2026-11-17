package br.com.cpet.dominio;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public final class Repositorios {
    private Repositorios() {
    }

    public interface CasoRepository extends JpaRepository<Caso, Long> {
        Optional<Caso> findByCodigoAcompanhamento(String codigo);

        List<Caso> findByStatusIn(Collection<StatusCaso> status);

        List<Caso> findByRecursoIdAndStatusIn(Long recursoId, Collection<StatusCaso> status);

        List<Caso> findByAtualizadoEmAfter(LocalDateTime desde);

        boolean existsByCodigoAcompanhamento(String codigo);

        long countBySimulado(int simulado);
    }

    public interface ReporteRepository extends JpaRepository<Reporte, Long> {
        List<Reporte> findByCasoIdOrderByCriadoEmAscIdAsc(Long casoId);

        long countByCasoId(Long casoId);
    }

    public interface HistoricoRepository extends JpaRepository<HistoricoStatus, Long> {
        List<HistoricoStatus> findByCasoIdOrderByCriadoEmAscIdAsc(Long casoId);
    }

    public interface RecursoRepository extends JpaRepository<Recurso, Long> {
    }

    public interface BairroRepository extends JpaRepository<Bairro, Long> {
    }

    public interface CampanhaRepository extends JpaRepository<Campanha, Long> {
        List<Campanha> findByCasoIdOrderByIdDesc(Long casoId);
    }

    public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    }
}
