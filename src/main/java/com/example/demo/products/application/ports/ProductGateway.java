package com.example.demo.products.application.ports;

import com.example.demo.products.domain.models.Product;

import java.util.List;

public interface ProductGateway {

    List<Product> getProducts();
}
