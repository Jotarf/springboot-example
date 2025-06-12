package com.example.demo.products.application.usecases;

import com.example.demo.products.application.dtos.request.CreateProductBodyDto;
import com.example.demo.products.application.dtos.response.GetProductResponseDto;
import com.example.demo.products.application.ports.ProductRepository;
import com.example.demo.products.domain.models.Product;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class CreateProductUseCase {

    private final ProductRepository productRepository;

    public CreateProductUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public GetProductResponseDto execute(CreateProductBodyDto createProductBodyDto) throws Exception {

        Product product = new Product();
        product.setName(createProductBodyDto.getName());
        product.setQuantity(createProductBodyDto.getQuantity());
        product.setPrice(createProductBodyDto.getPrice());

        Optional<Product> createdProduct = this.productRepository.createProduct(product);

        if(createdProduct.isEmpty()) throw new Exception("Error creating product");

        return new GetProductResponseDto(createdProduct.get());
    }
}
