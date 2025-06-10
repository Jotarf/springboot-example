package com.example.demo.products.application.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
@AllArgsConstructor
public class CreateProductRequest {

    @NotBlank(message = "Product name can't be blank")
    @Length(min = 3, max = 50, message = "Product name length must be min 3 characters max 50 characters")
    private String name;

    @Positive(message = "Product price must be greater than zero")
    private Double price;

    @PositiveOrZero(message = "Product quantity must be equal or greater than zero")
    private Long quantity;
}
