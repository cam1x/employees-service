package com.epam.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class ElasticSearchConfig {

    @Value("${elasticsearch.protocol}")
    private String elasticServerProtocol;

    @Value("${elasticsearch.url}")
    private String elasticServerUrl;

    @Value("${elasticsearch.port}")
    private Integer elasticServerPort;

    @Bean
    public RestClient restClient() throws IOException {
        return RestClient.builder(new HttpHost(elasticServerUrl, elasticServerPort, elasticServerProtocol))
                .build();
    }

    @Bean
    public ElasticsearchClient elasticsearchClient() throws IOException {
        var transport = new RestClientTransport(restClient(), new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}
