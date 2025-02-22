package com.mylearning.productmicroservices.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.util.Map;

@Configuration
public class KafkaConfig {

//    @Bean
//    public NewTopic createTopic() {
//        return new NewTopic("product", 1, (short) 1);
//    }

    @Bean
    public NewTopic createTopic() {
        return TopicBuilder.name("product-created-event-topic")
                .replicas(1)
                .configs(Map.of("min.insync.replicas", "1"))
                .partitions(1)
                .build();
    }

}
