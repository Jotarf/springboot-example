package com.example.demo.products.application.ports;

import com.example.demo.products.domain.models.Product;

import java.util.List;
import java.util.Optional;

public interface ProductGateway {
    List<Product> getProducts();
    Optional<Product> createProduct(Product product);
}
