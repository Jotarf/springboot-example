package com.example.demo.common.domain.criteria;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CriteriaJoin {
    private final String joinPath; // Field to join on in the parent entity
    private final JoinTypeCriteria joinTypeCriteria; // INNER, LEFT
}
