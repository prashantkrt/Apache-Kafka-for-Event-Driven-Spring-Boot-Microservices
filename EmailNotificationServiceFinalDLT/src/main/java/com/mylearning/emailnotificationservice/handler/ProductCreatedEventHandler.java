package com.mylearning.emailnotificationservice.handler;

import com.mylearning.ProductCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@KafkaListener(topics = {"product-created-event-topic", "topicA"}, groupId = "product-created-events")
public class ProductCreatedEventHandler {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private final RestTemplate restTemplate;

    public ProductCreatedEventHandler(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @KafkaHandler
    public void handle(ProductCreatedEvent productCreatedEvent) {
        LOGGER.info("Received a new event {} and product id is {}", productCreatedEvent.getTitle(), productCreatedEvent.getProductId());

        String requestUrl = "http://localhost:8083/response/2010";

        ResponseEntity<String> response = restTemplate.exchange(requestUrl, HttpMethod.GET, null, String.class);
        if (response.getStatusCode().value() == 200) {
            LOGGER.info("Received Response from the remote service {}", response.getBody());
        }
    }
}


//Better understanding of rest template exchange method
//  RestTemplate restTemplate = new RestTemplate();
//
//        // URL
//        String url = "https://api.example.com/products/{id}";
//
//        // Headers
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("Authorization", "Bearer your_token_here");
//        headers.setContentType(MediaType.APPLICATION_JSON);
//
//        // Request body (for POST/PUT requests)
//        Product requestBody = new Product("Laptop", 1200.0);
//
//        // HTTP Entity (body + headers)
//        HttpEntity<Product> entity = new HttpEntity<>(requestBody, headers);
//
//        // Make the exchange request
//        ResponseEntity<Product> response = restTemplate.exchange(
//            url,
//            HttpMethod.GET,
//            entity,
//            Product.class,
//            123 // URI path variable
//        );
//
//        // Handle response
//        if (response.getStatusCode() == HttpStatus.OK) {
//            Product product = response.getBody();
//            System.out.println("Product Name: " + product.getName());
//            System.out.println("Product Price: " + product.getPrice());
//        } else {
//            System.out.println("Failed with status code: " + response.getStatusCode());
//        }
//    }
