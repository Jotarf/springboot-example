package com.example.demo.products.application.ports;

import com.example.demo.common.application.dtos.PaginationResponseDto;
import com.example.demo.products.application.dtos.request.GetPaginatedProductsQueryDto;
import com.example.demo.products.domain.models.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    List<Product> getProducts();
    PaginationResponseDto<Product> getPaginatedProducts(GetPaginatedProductsQueryDto query);
    Optional<Product> createProduct(Product product);
}
