package com.example.demo.common.domain.criteria;

public enum SingleFilterOperator implements FilterOperator {
    EQUAL("="),
    NOT_EQUAL("!="),
    GT(">"),
    LT("<"),
    CONTAINS("CONTAINS"),
    NOT_CONTAINS("NOT_CONTAINS"),
    IN("IN"),
    NOT_IN("NOT_IN");

    private final String operator;

    SingleFilterOperator(String operator) {
        this.operator = operator;
    }

    public static SingleFilterOperator fromValue(String value) {
        switch (value) {
            case "=":
                return SingleFilterOperator.EQUAL;
            case "!=":
                return SingleFilterOperator.NOT_EQUAL;
            case ">":
                return SingleFilterOperator.GT;
            case "<":
                return SingleFilterOperator.LT;
            case "CONTAINS":
                return SingleFilterOperator.CONTAINS;
            case "NOT_CONTAINS":
                return SingleFilterOperator.NOT_CONTAINS;
            case "IN":
                return SingleFilterOperator.IN;
            case "NOT_IN":
                return SingleFilterOperator.NOT_IN;
            default:
                return null;
        }
    }

    public boolean isPositive() {
        return this != NOT_EQUAL && this != NOT_CONTAINS
                && this != NOT_IN;
    }

    public String value() {
        return operator;
    }
}
