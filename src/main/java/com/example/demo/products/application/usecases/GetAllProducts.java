package com.example.demo.products.application.usecases;

import com.example.demo.products.application.dtos.response.GetProductResponse;
import com.example.demo.products.application.ports.ProductGateway;
import com.example.demo.products.domain.models.Product;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetAllProducts {

    private final ProductGateway productGateway;

    public GetAllProducts(ProductGateway productGateway) {
        this.productGateway = productGateway;
    }

    public List<GetProductResponse> execute() {
        List<Product> products = this.productGateway.getProducts();
        return products.stream().map(GetProductResponse::new).toList();
    }
}
