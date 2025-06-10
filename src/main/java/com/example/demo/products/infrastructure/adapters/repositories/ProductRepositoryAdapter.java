package com.example.demo.products.infrastructure.adapters.repositories;

import com.example.demo.common.application.dtos.PaginationResponseDto;
import com.example.demo.products.application.dtos.request.GetPaginatedProductsQueryDto;
import com.example.demo.products.application.ports.ProductRepository;
import com.example.demo.products.domain.models.Product;
import com.example.demo.products.infrastructure.entities.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ProductRepositoryAdapter implements ProductRepository {

    private final JpaProductRepository productRepository;

    public ProductRepositoryAdapter(JpaProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public List<Product> getProducts() {
        List<ProductEntity> products = this.productRepository.findAll();
        return products.stream().map(ProductEntity::toModel).toList();
    }

    @Override
    public PaginationResponseDto<Product> getPaginatedProducts(GetPaginatedProductsQueryDto query) {
        Pageable pageable = PageRequest.of(query.page, query.limit);
        Page<ProductEntity> products = this.productRepository.findAll(pageable);

        return new PaginationResponseDto<>(
                products.getContent().stream().map(ProductEntity::toModel).toList(),
                query.page,
                query.limit,
                products.getTotalPages(),
                products.getTotalElements()
        );
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
