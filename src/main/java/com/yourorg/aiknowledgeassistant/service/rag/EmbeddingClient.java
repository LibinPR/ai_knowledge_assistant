package com.yourorg.aiknowledgeassistant.service.rag;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;

@Service
public class EmbeddingClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String embeddingUrl;

    public EmbeddingClient(
        @Value("${embedding.server.url:http://localhost:8081/embed}") String embeddingUrl
    ) {
        this.embeddingUrl = embeddingUrl;
    }

    public Float[] embed(String text) {

        Map<String , String> request = Map.of(
                "inputs" , "query:" + text
        );

        ResponseEntity<List> response =
                restTemplate.postForEntity(embeddingUrl , request , List.class);

        List<List<Double>> body = response.getBody();
        List<Double> vector = body.get(0);

        Float[] embedding = new Float[vector.size()];

        for(int i=0; i< vector.size(); i++) {
            embedding[i] = vector.get(i).floatValue();
        }

        return embedding;
    }
}

