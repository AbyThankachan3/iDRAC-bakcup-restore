package com.idrac.client;

import com.idrac.config.RedfishConnection;
import com.idrac.config.UnsafeFeignConfig;
import com.idrac.transport.FeignRedfishApi;
import feign.Feign;
import feign.RequestInterceptor;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;

import java.util.Base64;

public final class RedfishClientBuilder {

    private RedfishClientBuilder() {}

    public static DefaultRedfishClient build(RedfishConnection connection) {

        RequestInterceptor authInterceptor =
                template -> {
                    String auth = connection.getUsername() + ":" + connection.getPassword();
                    String encoded = Base64.getEncoder().encodeToString(auth.getBytes());
                    template.header(
                            "Authorization",
                            "Basic " + encoded
                    );
                    template.header("Accept", "*/*");
                };

        FeignRedfishApi api =
                Feign.builder()
                        .client(new OkHttpClient(UnsafeFeignConfig.createUnsafeOkHttpClient()))
                        .encoder(new JacksonEncoder())
                        .decoder(new JacksonDecoder())
                        .requestInterceptor(authInterceptor)
                        .target(FeignRedfishApi.class, connection.getBaseUrl());

        return new DefaultRedfishClient(connection, api);
    }
}
