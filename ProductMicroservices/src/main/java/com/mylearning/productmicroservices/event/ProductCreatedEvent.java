package com.mylearning.productmicroservices.event;

import java.math.BigDecimal;

public class ProductCreatedEvent {

    private String productId;
    private String title;
    private Integer quantity;
    private BigDecimal price;

    public ProductCreatedEvent() {
    }

    public ProductCreatedEvent(String productId, String title, Integer quantity, BigDecimal price) {
        this.productId = productId;
        this.title = title;
        this.quantity = quantity;
        this.price = price;
    }
}
