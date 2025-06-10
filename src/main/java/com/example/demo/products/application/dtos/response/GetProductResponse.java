package com.example.demo.products.application.dtos.response;

import com.example.demo.products.domain.models.Product;
import lombok.Data;

import java.util.UUID;

@Data
public class GetProductResponse {
    private UUID id;
    private String name;
    private Double price;
    private Long quantity;

    public GetProductResponse(Product product){
        this.id = product.getId();
        this.name = product.getName();
        this.price = product.getPrice();
        this.quantity = product.getQuantity();
    }
}
