package br.com.cpet;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import br.com.cpet.config.RegrasProperties;

/** Carrega as regras do application.yml real, para os testes validarem as constantes de verdade. */
public final class RegrasTeste {

    private RegrasTeste() {
    }

    public static RegrasProperties carregar() {
        try {
            StandardEnvironment env = new StandardEnvironment();
            new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"))
                    .forEach(env.getPropertySources()::addLast);
            return Binder.get(env).bind("cpet.regras", RegrasProperties.class).get();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
