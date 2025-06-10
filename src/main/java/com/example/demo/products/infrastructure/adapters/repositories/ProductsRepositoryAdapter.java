package com.example.demo.products.infrastructure.adapters.repositories;

import com.example.demo.products.application.ports.ProductGateway;
import com.example.demo.products.domain.models.Product;
import com.example.demo.products.infrastructure.entities.ProductEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ProductsRepositoryAdapter implements ProductGateway {

    private final JpaProductRepository productRepository;

    public ProductsRepositoryAdapter(JpaProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public List<Product> getProducts() {
        List<ProductEntity> products = this.productRepository.findAll();
        return products.stream().map(ProductEntity::toModel).toList();
    }

    @Override
    public Optional<Product> createProduct(Product product) {
        try {
            ProductEntity createdProduct = productRepository.save(new ProductEntity(product));
            return Optional.of(createdProduct.toModel());
        }catch(Exception ex){
            return Optional.empty();
        }
    }
}
