package com.mylearning.emailnotificationservice.handler;

import com.mylearning.emailnotificationservice.event.ProductCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@KafkaListener(topics = {"product-created-event-topic", "topicA"})
public class ProductCreatedEventHandler {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    //@KafkaListener(topics={"product-created-event-topic","topicA"})
    //better way do this way
    @KafkaHandler
    public void handle(ProductCreatedEvent productCreatedEvent) {
        LOGGER.info("Received a new event {}", productCreatedEvent.getTitle());

    }

}
