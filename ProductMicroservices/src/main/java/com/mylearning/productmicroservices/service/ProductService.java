package com.mylearning.productmicroservices.service;

import com.mylearning.productmicroservices.event.ProductCreatedEvent;
import com.mylearning.productmicroservices.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class ProductService {

    private final KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate;
    private final Logger LOGGER = LoggerFactory.getLogger(ProductService.class);

    public ProductService(KafkaTemplate kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public String createProduct(Product product) {

        String productId = UUID.randomUUID().toString().split("-")[0];
        ProductCreatedEvent productCreatedEvent = new ProductCreatedEvent(productId, product.getTitle(), product.getQuantity(), product.getPrice());

        //Asynchronous communication with Kafka but wanted to get notified
        CompletableFuture<SendResult<String, ProductCreatedEvent>> send = kafkaTemplate.send("product-created-event-topic", productId, productCreatedEvent);//topic key,event

        send.whenComplete((result, exception) -> {
            if (exception != null) {
                LOGGER.error("Product creation failed with id : "+productId + " exception : " + exception.getMessage());
                //exception.printStackTrace();
            } else {
                LOGGER.info("Product created successfully with id : " + result.getRecordMetadata());
                System.out.println("Product created successfully with id : " + productId);
            }
        });
        //send.join();//now its synchronous call
        //The code starts asynchronously, but because of send.join();
        // it waits for completion, making it effectively synchronous.
        LOGGER.info("Returning the product id : " + productId);

        return productId;
    }
}
