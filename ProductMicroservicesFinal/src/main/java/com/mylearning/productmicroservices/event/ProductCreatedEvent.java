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

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
