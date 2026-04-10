package com.yourorg.aiknowledgeassistant.service.rag;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class EmbeddingClient {

    private final RestTemplate restTemplate = new RestTemplate();

    public Float[] embed(String text) {
        String url = "http://localhost:8081/embed";

        Map<String , String> request = Map.of(
                "inputs" , "query:" + text
        );

        ResponseEntity<List> response =
                restTemplate.postForEntity(url , request , List.class);

        List<List<Double>> body = response.getBody();
        List<Double> vector = body.get(0);

        Float[] embedding = new Float[vector.size()];

        for(int i=0; i< vector.size(); i++) {
            embedding[i] = vector.get(i).floatValue();
        }

        return embedding;
    }
}

