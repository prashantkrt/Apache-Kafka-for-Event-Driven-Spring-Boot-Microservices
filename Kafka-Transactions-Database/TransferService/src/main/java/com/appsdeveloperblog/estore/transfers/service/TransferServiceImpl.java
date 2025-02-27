package com.appsdeveloperblog.estore.transfers.service;

import com.appsdeveloperblog.estore.transfers.entity.TransferEntity;
import com.appsdeveloperblog.estore.transfers.repository.TransferRepository;
import com.fasterxml.jackson.databind.util.BeanUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.appsdeveloperblog.estore.transfers.error.TransferServiceException;
import com.appsdeveloperblog.estore.transfers.model.TransferRestModel;
import com.appsdeveloperblog.payments.ws.core.events.DepositRequestedEvent;
import com.appsdeveloperblog.payments.ws.core.events.WithdrawalRequestedEvent;

import java.util.UUID;

@Service
public class TransferServiceImpl implements TransferService {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private KafkaTemplate<String, Object> kafkaTemplate;
    private Environment environment;
    private RestTemplate restTemplate;
    private TransferRepository transferRepository;

    public TransferServiceImpl(KafkaTemplate<String, Object> kafkaTemplate, Environment environment,
                               RestTemplate restTemplate, TransferRepository transferRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.environment = environment;
        this.restTemplate = restTemplate;
        this.transferRepository = transferRepository;
    }

    //by default, transaction rollback if runtime exception occurs
    //will have to replace the kafka transaction manager with the jpa transaction manager
    // this will work with both kafka and jpa
    @Transactional(value = "transactionManager")
    @Override
    public boolean transfer(TransferRestModel transferRestModel) {

        WithdrawalRequestedEvent withdrawalEvent = new WithdrawalRequestedEvent(transferRestModel.getSenderId(),
                transferRestModel.getRecepientId(), transferRestModel.getAmount());

        DepositRequestedEvent depositEvent = new DepositRequestedEvent(transferRestModel.getSenderId(),
                transferRestModel.getRecepientId(), transferRestModel.getAmount());

        //Suppose I wanted to save the record to the db
        TransferEntity data = new TransferEntity();
        BeanUtils.copyProperties(transferRestModel, data);//copy source to target
        data.setTransferId(UUID.randomUUID().toString().split("-")[0]);
        try {
            transferRepository.save(data);
            kafkaTemplate.send(environment.getProperty("withdraw-money-topic", "withdraw-money-topic"),
                    withdrawalEvent);
            LOGGER.info("Sent event to withdrawal topic.");

            // Business logic that causes and error
            // Business logic that causes and error
            // if we produce some error the transaction will rolled back
            // by default it rolls back unchecked exceptions(Runtime exceptions) and for errors
            // by default no rollback for checked exception, we have to configure it manually
            // use rooBackFor= for all unchecked exceptions
            // rollBackFor= for checked exceptions not required it by default behavior
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
        String requestUrl = "http://localhost:8082/response/2000";
        ResponseEntity<String> response = restTemplate.exchange(requestUrl, HttpMethod.GET, null, String.class);

        if (response.getStatusCode().value() == HttpStatus.SERVICE_UNAVAILABLE.value()) {
            throw new Exception("Destination Microservice not availble");
        }

        if (response.getStatusCode().value() == HttpStatus.OK.value()) {
            LOGGER.info("Received response from mock service: " + response.getBody());
        }
        return response;
    }

}
