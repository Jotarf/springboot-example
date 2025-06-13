package com.example.demo.products.application.usecases;

import com.example.demo.common.domain.criteria.Criteria;
import com.example.demo.common.domain.criteria.Order;
import com.example.demo.common.domain.criteria.SingleFilter;
import com.example.demo.common.domain.criteria.SingleFilterOperator;
import com.example.demo.common.domain.criteria.Filter;
import com.example.demo.products.application.dtos.request.GetProductsByCriteriaDto;
import com.example.demo.products.application.dtos.response.GetProductResponseDto;
import com.example.demo.products.application.ports.ProductRepository;
import com.example.demo.products.domain.models.Product;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GetProductsByCriteriaUseCase {

    private final ProductRepository productRepository;

    public GetProductsByCriteriaUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<GetProductResponseDto> execute(GetProductsByCriteriaDto query) {

        List<Filter> filters = new ArrayList<>();

        if(query.getName().isPresent()) {
            filters.add(new SingleFilter<>("name", SingleFilterOperator.EQUAL, query.getName().get()));
        }

        if(query.getHasStock().isPresent() && query.getHasStock().get()){
            filters.add(new SingleFilter<>("quantity", SingleFilterOperator.GT, 0L));
        }
        if(query.getHasStock().isPresent() && !query.getHasStock().get()){
            filters.add(new SingleFilter<>("quantity", SingleFilterOperator.EQUAL, 0L));
        }

        if(query.getMinPrice().isPresent()){
            filters.add(new SingleFilter<>("price", SingleFilterOperator.GT, query.getMinPrice().get()));
        }

        if(query.getMaxPrice().isPresent()){
            filters.add(new SingleFilter<>("price", SingleFilterOperator.LT, query.getMaxPrice().get()));
        }

        if(query.getQuantity().isPresent()){
            filters.add(new SingleFilter<>("quantity", SingleFilterOperator.EQUAL, query.getQuantity().get()));
        }

        Criteria criteria = new Criteria(filters, Order.none());

        List<Product> products = this.productRepository.getProductsByCriteriaSpecification(criteria);
        return products.stream().map(GetProductResponseDto::new).toList();
    }
}
