package com.example.demo.common.domain.criteria;

public enum OrderType {
    ASC("asc"),
    DESC("desc"),
    NONE("none");
    private final String type;

    OrderType(String type) {
        this.type = type;
    }

    public boolean isNone() {
        return this == NONE;
    }

    public boolean isAsc() {
        return this == ASC;
    }

    public String getType() {
        return type;
    }
}
