package com.appsdeveloperblog.estore.transfers.service;

import com.appsdeveloperblog.estore.transfers.error.TransferServiceException;
import com.appsdeveloperblog.estore.transfers.model.TransferRestModel;
import com.appsdeveloperblog.payments.ws.core.events.DepositRequestedEvent;
import com.appsdeveloperblog.payments.ws.core.events.WithdrawalRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.*;

import java.net.ConnectException;

@Service
public class TransferServiceImpl implements TransferService {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Environment environment;
    private final RestTemplate restTemplate;

    public TransferServiceImpl(KafkaTemplate<String, Object> kafkaTemplate, Environment environment,
                               RestTemplate restTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.environment = environment;
        this.restTemplate = restTemplate;
    }

    //if we use TransactionManager value here then it will happen transaction under transaction
    //I wanted to create independent local transaction here
    @Transactional(rollbackFor = {TransferServiceException.class, ConnectException.class})
    @Override
    public boolean transfer(TransferRestModel transferRestModel) {
        WithdrawalRequestedEvent withdrawalEvent = new WithdrawalRequestedEvent(
                transferRestModel.getSenderId(),
                transferRestModel.getRecepientId(),
                transferRestModel.getAmount()
        );

        DepositRequestedEvent depositEvent = new DepositRequestedEvent(
                transferRestModel.getSenderId(),
                transferRestModel.getRecepientId(),
                transferRestModel.getAmount()
        );

        // local transactions
        return kafkaTemplate.executeInTransaction(operations -> {
            try {
                operations.send(environment.getProperty("withdraw-money-topic", "withdraw-money-topic"), withdrawalEvent);
                LOGGER.info("Sent event to withdrawal topic.");

                callRemoteService();

                operations.send(environment.getProperty("deposit-money-topic", "deposit-money-topic"), depositEvent);
                LOGGER.info("Sent event to deposit topic.");

            } catch (Exception ex) {
                LOGGER.error("Transaction failed: {}", ex.getMessage(), ex);
                throw new TransferServiceException(ex);
            }
            return true;
        });
    }

    private ResponseEntity<String> callRemoteService() throws Exception {
        String requestUrl = "http://localhost:8082/response/200";

        ResponseEntity<String> response = null;

        try {
            response = restTemplate.exchange(requestUrl, HttpMethod.GET, null, String.class);
        } catch (HttpClientErrorException e) {
            LOGGER.error("Client error: {}", e.getStatusCode());
            throw new ConnectException("Client error: " + e.getStatusCode() + " - " + e.getStatusText());
        } catch (HttpServerErrorException e) {
            LOGGER.error("Server error: {}", e.getStatusCode());
            throw new ConnectException("Server error: " + e.getStatusCode() + " - " + e.getStatusText());
        } catch (ResourceAccessException e) {
            LOGGER.error("Connection error: {}", e.getMessage());
            throw new ConnectException("Connection error: " + e.getMessage());
        } catch (RestClientException e) {
            LOGGER.error("Other error: {}", e.getMessage());
            throw new ConnectException("Other error: " + e.getMessage());
        }

        if (response.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
            throw new Exception("Destination Microservice not available");
        }

        if (response.getStatusCode() == HttpStatus.OK) {
            LOGGER.info("Received response from remote service: {}", response.getBody());
        }
        return response;
    }
}



