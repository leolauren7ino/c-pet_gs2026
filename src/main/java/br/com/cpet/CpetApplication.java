package br.com.cpet;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableJpaRepositories(considerNestedRepositories = true)
public class CpetApplication {

    public static void main(String[] args) {
        SpringApplication.run(CpetApplication.class, args);
    }

    @Bean
    Clock relogio() {
        return Clock.systemDefaultZone();
    }
}
