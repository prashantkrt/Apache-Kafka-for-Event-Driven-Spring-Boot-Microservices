package com.mylearning.productmicroservices.service;

import com.mylearning.productmicroservices.event.ProductCreatedEvent;
import com.mylearning.productmicroservices.model.Product;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProductService {

    private final KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate;

    public ProductService(KafkaTemplate kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public String createProduct(Product product) {

        String productId = UUID.randomUUID().toString().split("-")[0];
        ProductCreatedEvent productCreatedEvent = new ProductCreatedEvent(productId, product.getTitle(), product.getQuantity(), product.getPrice());

        kafkaTemplate.send("product-created-event-topic", productId, productCreatedEvent);//topic key,event

        return productId;
    }
}
