package com.example.demo.products.application.dtos.request;

import com.example.demo.common.application.dtos.PaginationQueryDto;


public class GetPaginatedProductsQueryDto extends PaginationQueryDto {
    public GetPaginatedProductsQueryDto(Integer limit, Integer page) {
        super(limit, page);
    }
}
