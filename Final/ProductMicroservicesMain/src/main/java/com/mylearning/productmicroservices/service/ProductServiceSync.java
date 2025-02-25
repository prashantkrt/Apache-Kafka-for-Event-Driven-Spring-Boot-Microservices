package com.mylearning.productmicroservices.service;

import com.mylearning.ProductCreatedEvent;
import com.mylearning.productmicroservices.model.Product;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class ProductServiceSync {

    private final Logger LOGGER = LoggerFactory.getLogger(ProductServiceSync.class);

    private final KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate;

    public ProductServiceSync(KafkaTemplate kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    //This is one way
    public String createProduct(Product product) {
        String productId = UUID.randomUUID().toString();
        ProductCreatedEvent productCreatedEvent = new ProductCreatedEvent(productId, product.getTitle(), product.getQuantity(), product.getPrice());

            CompletableFuture<SendResult<String,ProductCreatedEvent>> send = kafkaTemplate.send("product-created-event-topic", productId, productCreatedEvent);

            send.whenComplete((result, exception) -> {
                if (exception != null) {
                    LOGGER.error("Product creation failed with id : {} exception : {}", productId, exception.getMessage());
                } else {
                    LOGGER.info("Product created successfully with id : {}", result.getRecordMetadata());
                }
            });
            send.join();//sync call
          //This means the thread will wait until the completable future is completed
        return productId;
    }

    //Better Approach
    public String createProductSync(Product product) {
        String productId = UUID.randomUUID().toString();
        ProductCreatedEvent productCreatedEvent = new ProductCreatedEvent(productId, product.getTitle(), product.getQuantity(), product.getPrice());
        LOGGER.info("Before publishing an Event");
        try {
            //SendResult<String, ProductCreatedEvent> result = kafkaTemplate.send("product-created-event-topic", productId, productCreatedEvent).get();
            ProducerRecord<String, ProductCreatedEvent> productRecord = new ProducerRecord<>("product-created-event-topic",productId,productCreatedEvent);
            productRecord.headers().add("messageId", UUID.randomUUID().toString().getBytes());
            SendResult<String, ProductCreatedEvent> result = kafkaTemplate.send(productRecord).get();

            LOGGER.info("After publishing an Event => partition, => offset, => topic, => timestamp :{},{} ,{},{} ",result.getRecordMetadata().partition(),result.getRecordMetadata().offset(),result.getRecordMetadata().topic(),result.getRecordMetadata().timestamp());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        LOGGER.info("Returning the product with product id as : {}", productId);
        return productId;
    }
}
