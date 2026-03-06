package com.idrac.backup.config;

import feign.RequestInterceptor;
import lombok.Builder;
import lombok.Data;
import org.springframework.context.annotation.Bean;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Data
@Builder
public class FeignConfig {

    private final RedfishConnection connection;

    public FeignConfig(RedfishConnection connection) {
        this.connection = connection;
    }

    @Bean
    public RequestInterceptor authInterceptor() {

        String auth = connection.getUsername() + ":" + connection.getPassword();
        String encoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        return template -> {

            template.header("Authorization", "Basic " + encoded);
            template.header("Accept", "*/*");
            template.header("Content-Type", "application/json");
        };
    }

}