package com.idrac.config;

import okhttp3.OkHttpClient;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

public final class UnsafeFeignConfig {

    private UnsafeFeignConfig() {}

    public static OkHttpClient createUnsafeOkHttpClient() {

        try {

            // Trust all certificates
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {

                        @Override
                        public void checkClientTrusted(
                                X509Certificate[] chain,
                                String authType
                        ) {}

                        @Override
                        public void checkServerTrusted(
                                X509Certificate[] chain,
                                String authType
                        ) {}

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };

            SSLContext sslContext =
                    SSLContext.getInstance("TLS");

            sslContext.init(
                    null,
                    trustAllCerts,
                    new java.security.SecureRandom()
            );

            SSLSocketFactory sslSocketFactory =
                    sslContext.getSocketFactory();

            return new OkHttpClient.Builder()

                    .sslSocketFactory(
                            sslSocketFactory,
                            (X509TrustManager) trustAllCerts[0]
                    )

                    .hostnameVerifier(
                            (hostname, session) -> true
                    )

                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(300, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)

                    .build();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to create unsafe OkHttpClient for iDRAC",
                    e
            );
        }
    }
}