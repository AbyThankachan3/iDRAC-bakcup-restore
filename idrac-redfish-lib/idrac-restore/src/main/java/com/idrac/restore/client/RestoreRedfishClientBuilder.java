package com.idrac.restore.client;

import com.idrac.restore.config.FeignConfig;
import com.idrac.restore.config.UnsafeFeignConfig;
import com.idrac.restore.model.RedfishConnection;
import com.idrac.restore.transport.FeignRestoreApi;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;

public class RestoreRedfishClientBuilder {

    public static RestoreClient build(RedfishConnection connection) {
        FeignConfig config = FeignConfig.builder()
                        .connection(connection)
                        .build();

        FeignRestoreApi api = Feign.builder()
                        .encoder(new JacksonEncoder())
                        .decoder(new JacksonDecoder())
                        .client(new OkHttpClient(UnsafeFeignConfig.createUnsafeOkHttpClient()))
                        .requestInterceptor(config.authInterceptor())
                        .target(FeignRestoreApi.class, connection.getBaseUrl());

        return new DefaultRestoreClient(api, connection);
    }
}
