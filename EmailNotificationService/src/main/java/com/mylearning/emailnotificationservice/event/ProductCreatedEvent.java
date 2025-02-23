package com.mylearning.emailnotificationservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ProductCreatedEvent {
    private String productId;
    private String title;
    private Integer quantity;
    private BigDecimal price;
}
