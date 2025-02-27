package com.appsdeveloperblog.estore.transfers.service;

import com.appsdeveloperblog.payments.ws.core.error.SpecificException;
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

import com.appsdeveloperblog.estore.transfers.error.TransferServiceException;
import com.appsdeveloperblog.estore.transfers.model.TransferRestModel;
import com.appsdeveloperblog.payments.ws.core.events.DepositRequestedEvent;
import com.appsdeveloperblog.payments.ws.core.events.WithdrawalRequestedEvent;

import java.net.ConnectException;

@Service
public class TransferServiceImpl implements TransferService {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private KafkaTemplate<String, Object> kafkaTemplate;
    private Environment environment;
    private RestTemplate restTemplate;

    public TransferServiceImpl(KafkaTemplate<String, Object> kafkaTemplate, Environment environment,
                               RestTemplate restTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.environment = environment;
        this.restTemplate = restTemplate;
    }

    //@Transactional //if we have a single transaction manager for your kafka
    // if you have multiple transaction manager, and then we can specific transaction manager
    @Transactional(value = "kafkaTransactionManager", rollbackFor = {TransferServiceException.class, ConnectException.class}, noRollbackFor = {SpecificException.class})
    @Override
    public boolean transfer(TransferRestModel transferRestModel) {
        WithdrawalRequestedEvent withdrawalEvent = new WithdrawalRequestedEvent(transferRestModel.getSenderId(),
                transferRestModel.getRecepientId(), transferRestModel.getAmount());
        DepositRequestedEvent depositEvent = new DepositRequestedEvent(transferRestModel.getSenderId(),
                transferRestModel.getRecepientId(), transferRestModel.getAmount());

        try {
            kafkaTemplate.send(environment.getProperty("withdraw-money-topic", "withdraw-money-topic"),
                    withdrawalEvent);
            LOGGER.info("Sent event to withdrawal topic.");

            // Business logic that causes and error
            // if we produce some error the transaction will rolled back
            // by default it rolls back unchecked exceptions(Runtime exceptions) and for errors
            // by default no rollback for checked exception, we have to configure it manually
            // use rooBackFor= for all unchecked exceptions
            // rollBackFor= for checked exceptions not required it by default behavir
            // noRollbackFor = {SpecificException.class}
            callRemoteServce();

            kafkaTemplate.send(environment.getProperty("deposit-money-topic", "deposit-money-topic"), depositEvent);
            LOGGER.info("Sent event to deposit topic");

        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            throw new TransferServiceException(ex);
        }

        return true;
    }

    private ResponseEntity<String> callRemoteServce() throws Exception {
        String requestUrl = "http://localhost:8082/response/200";

        ResponseEntity<String> response = null;
        try {
            response = restTemplate.exchange(requestUrl, HttpMethod.GET, null, String.class);
        } catch (HttpClientErrorException e) {
            System.out.println("Client error: " + e.getStatusCode());
            throw new ConnectException(e.getMessage());
        } catch (HttpServerErrorException e) {
            System.out.println("Server error: " + e.getStatusCode());
            throw new ConnectException(e.getMessage());
        } catch (ResourceAccessException e) {
            System.out.println("Connection error: " + e.getMessage());
            throw new ConnectException(e.getMessage());
        } catch (RestClientException e) {
            System.out.println("Other error: " + e.getMessage());
            throw new ConnectException(e.getMessage());
        }

        //ResourceAccessException → For connection issues (like timeouts) or invalid url
        //HttpClientErrorException → For 4xx HTTP status codes (like 404, 401).
        //HttpServerErrorException → For 5xx HTTP status codes (like 500).
        //UnknownHttpStatusCodeException → For unrecognized status codes.


        //Because the SERVICE_UNAVAILABLE check happens after you successfully get a response.
        //Imagine a scenario where:
        //The server returned a 200 OK, but the response body itself contains metadata that signals the service is actually down or degraded.
        //Or you’ve configured the RestTemplate to not throw exceptions for error responses, so you get a response object even on 5xx errors.
        if (response.getStatusCode().value() == HttpStatus.SERVICE_UNAVAILABLE.value()) { //503
            throw new Exception("Destination Microservice not available");
        }

        if (response.getStatusCode().value() == HttpStatus.OK.value()) {
            LOGGER.info("Received response from mock service: " + response.getBody());
        }
        return response;
    }

}
