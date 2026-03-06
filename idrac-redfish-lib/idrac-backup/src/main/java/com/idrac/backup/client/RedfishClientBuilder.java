package com.idrac.backup.client;


import com.idrac.backup.config.FeignConfig;
import com.idrac.backup.config.RedfishConnection;
import com.idrac.backup.config.UnsafeFeignConfig;
import com.idrac.backup.transport.FeignRedfishApi;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;

public final class RedfishClientBuilder {

    private RedfishClientBuilder() {}

    public static DefaultRedfishClient build(RedfishConnection connection) {
        FeignConfig config = FeignConfig.builder()
                .connection(connection)
                .build();

        FeignRedfishApi api =
                Feign.builder()
                        .client(new OkHttpClient(UnsafeFeignConfig.createUnsafeOkHttpClient()))
                        .encoder(new JacksonEncoder())
                        .decoder(new JacksonDecoder())
                        .requestInterceptor(config.authInterceptor())
                        .target(FeignRedfishApi.class, connection.getBaseUrl());

        return new DefaultRedfishClient(api);
    }
}
