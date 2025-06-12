package com.example.demo.products.infrastructure.adapters.repositories;

import com.example.demo.common.application.dtos.PaginationResponseDto;
import com.example.demo.common.domain.criteria.Criteria;
import com.example.demo.common.infrastructure.criteria.HibernateCriteriaConverter;
import com.example.demo.products.application.dtos.request.GetPaginatedProductsQueryDto;
import com.example.demo.products.application.ports.ProductRepository;
import com.example.demo.products.domain.models.Product;
import com.example.demo.products.infrastructure.entities.ProductEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ProductRepositoryAdapter implements ProductRepository {

    private final JpaProductRepository productRepository;
    private final HibernateCriteriaConverter<ProductEntity> hibernateCriteriaConverter;

    public ProductRepositoryAdapter(
            JpaProductRepository productRepository,
            HibernateCriteriaConverter<ProductEntity> hibernateCriteriaConverter
    ) {
        this.productRepository = productRepository;
        this.hibernateCriteriaConverter = hibernateCriteriaConverter;
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

    @Override
    public List<Product> getProductsByCriteria(Criteria criteria) {
        TypedQuery<ProductEntity> query = hibernateCriteriaConverter.convert(criteria, ProductEntity.class);

        return query.getResultList().stream()
                .map(ProductEntity::toModel)
                .toList();
    }
}
