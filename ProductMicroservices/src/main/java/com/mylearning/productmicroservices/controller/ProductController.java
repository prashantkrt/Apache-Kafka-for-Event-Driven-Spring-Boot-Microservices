package com.mylearning.productmicroservices.controller;

import com.mylearning.productmicroservices.event.ErrorMessage;
import com.mylearning.productmicroservices.model.Product;
import com.mylearning.productmicroservices.service.ProductServiceAsync;
import com.mylearning.productmicroservices.service.ProductServiceSync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private final ProductServiceAsync productServiceAsync;
    private final ProductServiceSync productServiceSync;

    public ProductController(ProductServiceAsync productServiceAsync, ProductServiceSync productServiceSync) {
        this.productServiceAsync = productServiceAsync;
        this.productServiceSync = productServiceSync;
    }

    @PostMapping("/async")
    public ResponseEntity<String> createProductAsync(@RequestBody Product product) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productServiceAsync.createProduct(product));
    }

    @PostMapping("/sync")
    public ResponseEntity<Object> createProductSync(@RequestBody Product product) {
        String productId;
        try {
            productId = productServiceSync.createProduct(product);
        } catch (Exception e) {
            LOGGER.error("Product creation failed new product {} exception ", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ErrorMessage(new Date(), e.getMessage(), "/products", e.getStackTrace().toString())
            );
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(productId);
    }

}
