package com.example.demo.common.domain.criteria;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CompoundFilter implements Filter {
    private CompoundFilterOperator operator; // "AND", "OR"
    private List<Filter> filters;
}
