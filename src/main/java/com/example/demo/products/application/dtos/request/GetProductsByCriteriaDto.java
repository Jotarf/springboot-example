package com.example.demo.products.application.dtos.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Value;

import java.util.Optional;


@Value
public class GetProductsByCriteriaDto {

    @Size(min = 1, max = 50, message = "Product name filter length must be min 3 characters max 50 characters ")
    String name;

    @Positive(message = "Product max price filter must be higher than zero")
    Double maxPrice;

    @Positive(message = "Product max price filter must be higher than zero")
    Double minPrice;

    Boolean hasStock;

    @PositiveOrZero(message = "Product quantity filter must be zero or higher")
    Long quantity;

    @AssertTrue(message = "The minimum price cannot be higher than the maximum price")
    public boolean isPriceRangeValid() {
        if (minPrice == null || maxPrice == null) return true;
        return minPrice <= maxPrice;
    }

    public Optional<String> getName(){
        return Optional.ofNullable(name);
    }

    public Optional<Boolean> getHasStock(){
        return Optional.ofNullable(hasStock);
    }

    public Optional<Double> getMaxPrice(){
        return Optional.ofNullable(maxPrice);
    }

    public Optional<Double> getMinPrice(){
        return Optional.ofNullable(minPrice);
    }

    public Optional<Long> getQuantity(){
        return Optional.ofNullable(quantity);
    }
}
