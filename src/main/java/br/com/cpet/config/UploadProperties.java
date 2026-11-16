package br.com.cpet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "cpet.upload")
public class UploadProperties {
    private String pasta = "./uploads";
    private long maxBytes = 5 * 1024 * 1024;
}
