package com.example.demo.products.application.usecases;

import com.example.demo.common.application.dtos.PaginationResponseDto;
import com.example.demo.products.application.dtos.request.GetPaginatedProductsQueryDto;
import com.example.demo.products.application.dtos.response.GetProductResponse;
import com.example.demo.products.application.ports.ProductGateway;
import com.example.demo.products.domain.models.Product;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetPaginatedProducts {

    private final ProductGateway productGateway;

    public GetPaginatedProducts(ProductGateway productGateway) {
        this.productGateway = productGateway;
    }

    public PaginationResponseDto<GetProductResponse> execute(GetPaginatedProductsQueryDto query) {
        PaginationResponseDto<Product> result = this.productGateway.getPaginatedProducts(query);

        return new PaginationResponseDto<>(
                result.getContent().stream().map(GetProductResponse::new).toList(),
                result.getPageNumber(),
                result.getPageSize(),
                result.getTotalPages(),
                result.getTotalElements()
        );
    }
}
