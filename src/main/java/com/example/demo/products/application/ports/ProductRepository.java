package com.example.demo.products.application.ports;

import com.example.demo.common.application.dtos.PaginationResponseDto;
import com.example.demo.common.domain.criteria.Criteria;
import com.example.demo.products.application.dtos.request.GetPaginatedProductsQueryDto;
import com.example.demo.products.domain.models.Product;
import com.example.demo.products.infrastructure.entities.ProductEntity;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    List<Product> getProducts();
    PaginationResponseDto<Product> getPaginatedProducts(GetPaginatedProductsQueryDto query);
    Optional<Product> createProduct(Product product);
    List<Product> getProductsByCriteria(Criteria criteria);
    List<Product> getProductsByCriteriaSpecification(Criteria criteria);
}
