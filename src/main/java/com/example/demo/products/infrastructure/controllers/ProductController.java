package com.example.demo.products.infrastructure.controllers;


import com.example.demo.common.application.dtos.PaginationResponseDto;
import com.example.demo.products.application.dtos.request.CreateProductBodyDto;
import com.example.demo.products.application.dtos.request.GetPaginatedProductsQueryDto;
import com.example.demo.products.application.dtos.request.GetProductsByCriteriaDto;
import com.example.demo.products.application.dtos.response.GetProductResponseDto;
import com.example.demo.products.application.usecases.CreateProductUseCase;
import com.example.demo.products.application.usecases.GetAllProductsUseCase;
import com.example.demo.products.application.usecases.GetPaginatedProductsUseCase;
import com.example.demo.products.application.usecases.GetProductsByCriteriaUseCase;
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

    private final GetAllProductsUseCase getAllProductsUseCase;
    private final CreateProductUseCase createProductUseCase;
    private final GetPaginatedProductsUseCase getPaginatedProductsUseCase;
    private final GetProductsByCriteriaUseCase getProductsByCriteriaUseCase;

    public ProductController(
            GetAllProductsUseCase getAllProductsUseCase,
            CreateProductUseCase createProductUseCase,
            GetPaginatedProductsUseCase getPaginatedProductsUseCase,
            GetProductsByCriteriaUseCase getProductsByCriteriaUseCase
    ) {
        this.getAllProductsUseCase = getAllProductsUseCase;
        this.createProductUseCase = createProductUseCase;
        this.getPaginatedProductsUseCase = getPaginatedProductsUseCase;
        this.getProductsByCriteriaUseCase = getProductsByCriteriaUseCase;
    }

    @GetMapping
    public ResponseEntity<List<GetProductResponseDto>> getProducts() {
        List<GetProductResponseDto> products = this.getAllProductsUseCase.execute();
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @GetMapping("paginated")
    public ResponseEntity<PaginationResponseDto<GetProductResponseDto>> getPaginatedProducts(
            @Valid @ModelAttribute GetPaginatedProductsQueryDto query
    ) {
        PaginationResponseDto<GetProductResponseDto> products = this.getPaginatedProductsUseCase.execute(query);
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @GetMapping("criteria")
    public ResponseEntity<List<GetProductResponseDto>> getProductsByCriteria(
            @Valid @ModelAttribute GetProductsByCriteriaDto query
    ) {
        List<GetProductResponseDto> products = this.getProductsByCriteriaUseCase.execute(query);
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<GetProductResponseDto> createProduct(
            @Valid @RequestBody CreateProductBodyDto createProductBodyDto
    ) throws Exception {
        try {
        GetProductResponseDto product = this.createProductUseCase.execute(createProductBodyDto);
        return new ResponseEntity<>(product, HttpStatus.CREATED);

        }catch(Exception ex){
            log.error("Error creating product");
            throw ex;
        }
    }
}
