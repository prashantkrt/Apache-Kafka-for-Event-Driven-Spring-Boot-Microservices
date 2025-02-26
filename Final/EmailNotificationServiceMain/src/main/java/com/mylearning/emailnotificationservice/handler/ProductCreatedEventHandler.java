package com.mylearning.emailnotificationservice.handler;

import com.mylearning.ProductCreatedEvent;
import com.mylearning.emailnotificationservice.entity.ProcessEventEntity;
import com.mylearning.emailnotificationservice.error.NonRetryableException;
import com.mylearning.emailnotificationservice.error.RetryableException;
import com.mylearning.emailnotificationservice.repository.ProcessEventEntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Component
@KafkaListener(topics = {"product-created-event-topic", "topicA"}, groupId = "product-created-events")
public class ProductCreatedEventHandler {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private final RestTemplate restTemplate;
    private final ProcessEventEntityRepository processEventEntityRepository;

    public ProductCreatedEventHandler(RestTemplate restTemplate, ProcessEventEntityRepository processEventEntityRepository) {
        this.restTemplate = restTemplate;
        this.processEventEntityRepository = processEventEntityRepository;
    }

    @Transactional
    @KafkaHandler
    public void handle(@Payload ProductCreatedEvent productCreatedEvent, @Header(value = "messageId", required = true) String messageId,
                       @Header(value = KafkaHeaders.RECEIVED_KEY, required = true) String messageKey) {
        LOGGER.info("Received a new event {} and product id is {}", productCreatedEvent.getTitle(), productCreatedEvent.getProductId());


        //check if the message was already processed here!!
        Optional<ProcessEventEntity> byMessageId = processEventEntityRepository.findByMessageId(messageId);
        if (byMessageId.isPresent()) {
            LOGGER.info("Duplicate event detected, skipping save: {}", messageId);
            return;
            //both works but for clean completion without any exceptions, we use return;
            // throw new NonRetryableException("Message already processed");
        }

        // Just return Returning Early (Your Current Approach):
        //Simple and fast — avoids unnecessary exception handling overhead.
        //No need to worry about retries or dead-letter queues (DLQ) if it's a duplicate.
        //Cleaner logs, no stack traces.

        //Throwing a Non-Retryable Exception:
        //Makes the duplication explicitly — easy to spot in monitoring systems.
        //Works well with Kafka error handling — you can route duplicates to a dead-letter topic for auditing.
        //Prevents accidental silent failures — duplicates cause failures, which can trigger alerts.

        String requestUrl = "http://localhost:8083/response/200";
        try {
            ResponseEntity<String> response = restTemplate.exchange(requestUrl, HttpMethod.GET, null, String.class);
            if (response.getStatusCode().value() == 200) {
                LOGGER.info("Received Response from the remote service {}", response.getBody());
            }
        } catch (ResourceAccessException ex) {
            LOGGER.error(ex.getMessage());
            throw new RetryableException(ex);
        } catch (HttpServerErrorException ex) {
            LOGGER.error(ex.getMessage());
            throw new NonRetryableException(ex);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
            throw new NonRetryableException(ex);
        }

        ProcessEventEntity processEventEntity = new ProcessEventEntity();
        processEventEntity.setMessageId(messageId);
        //processEventEntity.setProductId(productCreatedEvent.getProductId());
        processEventEntity.setProductId(messageKey);

        try {
            processEventEntityRepository.save(processEventEntity);
            // DataIntegrityViolationException is an exception in Spring Data that happens
            // when you try to perform a database operation that violates integrity constraints defined in your database.
            //Unique Constraint Violation (e.g., Duplicate Key)
            //Not Null Constraint Violation
            //Foreign Key Constraint Violation etc.
        } catch (DataIntegrityViolationException ex) {
            throw new NonRetryableException(ex);
        }
    }
}

//kafkaTemplate.send("product-topic", "12345", new ProductCreatedEvent("Laptop", 1000));
//messageId: (from headers)
//messageKey: 12345
//Payload: ProductCreatedEvent{name='Laptop', price=1000}

