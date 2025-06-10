package com.example.demo.products.infrastructure.controllers;


import com.example.demo.products.application.dtos.request.CreateProductRequest;
import com.example.demo.products.application.dtos.response.GetProductResponse;
import com.example.demo.products.application.usecases.CreateProduct;
import com.example.demo.products.application.usecases.GetAllProducts;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("products")
@Slf4j
public class ProductController {

    private final GetAllProducts getAllProducts;
    private final CreateProduct createProduct;

    public ProductController(GetAllProducts getAllProducts, CreateProduct createProduct) {
        this.getAllProducts = getAllProducts;
        this.createProduct = createProduct;
    }

    @GetMapping
    public ResponseEntity<List<GetProductResponse>> getProducts() {
        List<GetProductResponse> products = this.getAllProducts.execute();
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<GetProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest createProductRequest
    ) throws Exception {
        try {
        GetProductResponse product = this.createProduct.execute(createProductRequest);
        return new ResponseEntity<>(product, HttpStatus.CREATED);

        }catch(Exception ex){
            log.error("Error creating product");
            throw ex;
        }
    }
}
