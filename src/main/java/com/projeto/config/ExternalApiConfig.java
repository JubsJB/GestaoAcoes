package com.projeto.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class ExternalApiConfig {

    @Bean
    @Qualifier("brasilApiRestClient")
    public RestClient brasilApiRestClient(
            @Value("${integration.brasil-api.base-url}") String baseUrl,
            @Value("${integration.brasil-api.connect-timeout}") Duration connectTimeout,
            @Value("${integration.brasil-api.read-timeout}") Duration readTimeout
    ) {
        return createClient(baseUrl, connectTimeout, readTimeout);
    }

    @Bean
    @Qualifier("viaCepRestClient")
    public RestClient viaCepRestClient(
            @Value("${integration.via-cep.base-url}") String baseUrl,
            @Value("${integration.via-cep.connect-timeout}") Duration connectTimeout,
            @Value("${integration.via-cep.read-timeout}") Duration readTimeout
    ) {
        return createClient(baseUrl, connectTimeout, readTimeout);
    }

    private RestClient createClient(String baseUrl, Duration connectTimeout, Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
