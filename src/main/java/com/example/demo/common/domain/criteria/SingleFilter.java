package com.example.demo.common.domain.criteria;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SingleFilter<T> implements Filter {
    private final String field;
    private final SingleFilterOperator operator;
    private final T value;

    public String serialize() {
        return String.format("%s.%s.%s", field, operator.value(), value);
    }
}
