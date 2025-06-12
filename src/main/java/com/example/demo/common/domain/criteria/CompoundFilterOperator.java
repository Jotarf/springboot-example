package com.example.demo.common.domain.criteria;

public enum CompoundFilterOperator implements FilterOperator {
    AND("and"),
    OR("or");

    private final String operator;

    CompoundFilterOperator(String operator) {
        this.operator = operator;
    }

    public static CompoundFilterOperator fromValue(String value) {
        switch (value) {
            case "and":
                return CompoundFilterOperator.AND;
            case "or":
                return CompoundFilterOperator.OR;
            default:
                return null;
        }
    }

    public String value() {
        return operator;
    }
}
