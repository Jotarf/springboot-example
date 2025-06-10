package com.example.demo.products.infrastructure.controllers;


import com.example.demo.products.application.dtos.response.GetProductResponse;
import com.example.demo.products.application.usecases.GetAllProducts;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("products")
public class ProductController {

    private final GetAllProducts getAllProducts;

    public ProductController(GetAllProducts getAllProducts) {
        this.getAllProducts = getAllProducts;
    }

    @GetMapping
    public ResponseEntity<List<GetProductResponse>> getProducts() {
        List<GetProductResponse> products = this.getAllProducts.execute();
        return new ResponseEntity<>(products, HttpStatus.OK);
    }
}
