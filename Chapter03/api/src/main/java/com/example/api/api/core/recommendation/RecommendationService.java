package com.example.api.api.core.recommendation;

import jakarta.websocket.server.PathParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface RecommendationService {

    /**
     * Sample usage: "curl $HOST:$PORT/recommendation?productId=1".
     *
     * @param productId Id of the product
     * @return the recommendations of the product
     */

    @GetMapping(value = "/recommendation", produces = "application/json")
    public List<Recommendation> getRecommendations(@RequestParam(value = "productId", required = true) int productId);
}
