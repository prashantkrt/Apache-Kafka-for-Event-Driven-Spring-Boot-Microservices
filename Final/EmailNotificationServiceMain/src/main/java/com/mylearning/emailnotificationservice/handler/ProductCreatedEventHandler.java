package com.mylearning.emailnotificationservice.handler;

import com.mylearning.ProductCreatedEvent;
import com.mylearning.emailnotificationservice.error.NonRetryableException;
import com.mylearning.emailnotificationservice.error.RetryableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
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
    public void handle(@Payload ProductCreatedEvent productCreatedEvent, @Header(value="messageId",required = true) String messageId,
                       @Header(value = KafkaHeaders.RECEIVED_KEY , required = true) String MessageKey) {
        LOGGER.info("Received a new event {} and product id is {}", productCreatedEvent.getTitle(), productCreatedEvent.getProductId());

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
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
            throw new NonRetryableException(ex);
        }
    }
}

