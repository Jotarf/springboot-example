package com.example.demo.products.application.usecases;

import com.example.demo.products.application.dtos.response.GetProductResponseDto;
import com.example.demo.products.application.ports.ProductRepository;
import com.example.demo.products.domain.models.Product;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetAllProductsUseCase {

    private final ProductRepository productRepository;

    public GetAllProductsUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<GetProductResponseDto> execute() {
        List<Product> products = this.productRepository.getProducts();
        return products.stream().map(GetProductResponseDto::new).toList();
    }
}
