package com.example.demo.common.application.dtos;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.Optional;

public class PaginationQueryDto {
    @Digits(integer = 10, fraction = 0, message = "Limit must be an integer value when provided")
    @Min(value = 1, message = "Limit min value is 1'" )
    @Max(value = 100, message = "Limit max value is 100")
    public Integer limit;

    @Digits(integer = 10, fraction = 0, message = "Page must be an integer value when provided")
    @Min(value = 0, message = "Page min value is 0" )
    public Integer page;

    public PaginationQueryDto(Integer limit, Integer page){
        this.limit = Optional.ofNullable(limit).orElse(50);
        this.page = Optional.ofNullable(page).orElse(0);
    }
}
