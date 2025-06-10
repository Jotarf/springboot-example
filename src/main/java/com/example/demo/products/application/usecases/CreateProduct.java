package com.example.demo.products.application.usecases;

import com.example.demo.products.application.dtos.request.CreateProductRequest;
import com.example.demo.products.application.dtos.response.GetProductResponse;
import com.example.demo.products.application.ports.ProductGateway;
import com.example.demo.products.domain.models.Product;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class CreateProduct {

    private final ProductGateway productGateway;

    public CreateProduct(ProductGateway productGateway) {
        this.productGateway = productGateway;
    }

    public GetProductResponse execute(CreateProductRequest createProductRequest) throws Exception {

        Product product = new Product();
        product.setName(createProductRequest.getName());
        product.setQuantity(createProductRequest.getQuantity());
        product.setPrice(createProductRequest.getPrice());

        Optional<Product> createdProduct = this.productGateway.createProduct(product);

        if(createdProduct.isEmpty()) throw new Exception("Error creating product");

        return new GetProductResponse(createdProduct.get());
    }
}
