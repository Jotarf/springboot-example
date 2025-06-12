package com.example.demo.common.domain.criteria;

public enum JoinTypeCriteria {
    LEFT("left"),
    INNER("inner");
    private final String type;

    JoinTypeCriteria(String type) {
        this.type = type;
    }

    public boolean isLeft() {
        return this == LEFT;
    }

    public boolean isInner() {
        return this == INNER;
    }

    public String getType() {
        return type;
    }
}
