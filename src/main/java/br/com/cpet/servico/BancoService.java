package br.com.cpet.servico;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.cpet.dominio.Repositorios.CasoRepository;

/** Cria o schema se faltar, semeia na primeira subida e recria o seed sob demanda. */
@Service
public class BancoService implements ApplicationRunner {

    private static final String[] TABELAS_EM_ORDEM_DE_LIMPEZA = {
        "HISTORICO_STATUS", "REPORTE", "CASO", "USUARIO", "RECURSO", "BAIRRO"
    };

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;
    private final CasoService casoService;
    private final CasoRepository casos;
    private final TransactionTemplate transacao;

    public BancoService(DataSource dataSource, JdbcTemplate jdbc, CasoService casoService, CasoRepository casos,
                        PlatformTransactionManager gerenteTransacao) {
        this.dataSource = dataSource;
        this.jdbc = jdbc;
        this.casoService = casoService;
        this.casos = casos;
        this.transacao = new TransactionTemplate(gerenteTransacao);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!tabelaExiste("CASO")) {
            executar("db/schema.sql");
        }
        Integer bairros = jdbc.queryForObject("SELECT COUNT(*) FROM BAIRRO", Integer.class);
        if (bairros == null || bairros == 0) {
            semear();
        }
    }

    /** Limpeza e seed na mesma transação: se algo falhar, o banco volta ao estado anterior. */
    public void recriarSeed() {
        transacao.executeWithoutResult(status -> {
            for (String tabela : TABELAS_EM_ORDEM_DE_LIMPEZA) {
                jdbc.update("DELETE FROM " + tabela);
            }
            semearNaTransacao();
        });
    }

    private void semear() {
        transacao.executeWithoutResult(status -> semearNaTransacao());
    }

    private void semearNaTransacao() {
        executar("db/seed.sql");
        casoService.recalcularTodos();
        casos.flush();
    }

    private void executar(String recurso) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource(recurso));
        populator.setSqlScriptEncoding("UTF-8");
        populator.execute(dataSource);
    }

    private boolean tabelaExiste(String tabela) throws SQLException {
        try (Connection c = dataSource.getConnection();
             ResultSet rs = c.getMetaData().getTables(null, c.getSchema(), tabela, new String[] {"TABLE"})) {
            return rs.next();
        }
    }
}
