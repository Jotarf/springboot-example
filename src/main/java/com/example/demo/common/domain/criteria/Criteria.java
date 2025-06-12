package com.example.demo.common.domain.criteria;

import lombok.Getter;

import java.util.List;

@Getter
public class Criteria {
    private final List<Filter> filters;
    private final List<CriteriaJoin> criteriaJoins;
    private final Order order;
    private final Integer limit;
    private final Integer offset;

    public Criteria(List<Filter> filters
            , Order order,
                    List<CriteriaJoin> criteriaJoins
            , Integer limit, Integer offset
    ) {
        this.filters = filters;
        this.order = order;
        this.criteriaJoins = criteriaJoins;
        this.limit = limit;
        this.offset = offset;
    }

    public Criteria(List<Filter> filters
            , Order order
            , Integer limit, Integer offset
    ) {
        this.filters = filters;
        this.order = order;
        this.criteriaJoins = null;
        this.limit = limit;
        this.offset = offset;
    }

    public Criteria(List<Filter> filters
            , Order order

    ) {
        this.filters = filters;
        this.order = order;
        this.criteriaJoins = null;
        this.limit = null;
        this.offset = null;
    }

    public Criteria(List<Filter> filters
            , Order order,
                    List<CriteriaJoin> criteriaJoins

    ) {
        this.filters = filters;
        this.order = order;
        this.criteriaJoins = criteriaJoins;
        this.limit = null;
        this.offset = null;
    }

    public boolean hasFilters() {
        return !filters.isEmpty();
    }

    public void addFilter(Filter filter) {
        filters.add(filter);
    }
}
