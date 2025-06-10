package com.example.demo.products.application.usecases;

import com.example.demo.common.application.dtos.PaginationResponseDto;
import com.example.demo.products.application.dtos.request.GetPaginatedProductsQueryDto;
import com.example.demo.products.application.dtos.response.GetProductResponseDto;
import com.example.demo.products.application.ports.ProductRepository;
import com.example.demo.products.domain.models.Product;
import org.springframework.stereotype.Service;

@Service
public class GetPaginatedProductsUseCase {

    private final ProductRepository productRepository;

    public GetPaginatedProductsUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public PaginationResponseDto<GetProductResponseDto> execute(GetPaginatedProductsQueryDto query) {
        PaginationResponseDto<Product> result = this.productRepository.getPaginatedProducts(query);

        return new PaginationResponseDto<>(
                result.getContent().stream().map(GetProductResponseDto::new).toList(),
                result.getPageNumber(),
                result.getPageSize(),
                result.getTotalPages(),
                result.getTotalElements()
        );
    }
}
