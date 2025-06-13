package com.example.demo.common.infrastructure.criteria;


import com.example.demo.common.domain.criteria.Criteria;
import com.example.demo.common.domain.criteria.CompoundFilter;
import com.example.demo.common.domain.criteria.CompoundFilterOperator;
import com.example.demo.common.domain.criteria.Filter;
import com.example.demo.common.domain.criteria.CriteriaJoin;
import com.example.demo.common.domain.criteria.SingleFilter;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

public class CriteriaSpecificationConverter<T> implements Specification<T> {

    private final Criteria criteria;
    private final PredicateFactory predicateFactory;
    private final Map<Join<?, ?>, From<?, ?>> joinParents = new HashMap<>(); // Used for tracking parent joins

    public CriteriaSpecificationConverter(Criteria criteria, PredicateFactory predicateFactory) {
        this.criteria = criteria;
        this.predicateFactory = predicateFactory;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        // Handle joins first, as predicates might rely on them
        List<Join<?, ?>> joins = buildJoins(criteria.getCriteriaJoins(), root, query);

        // Handle ordering if present
        if (criteria.getOrder().hasOrder()) {
            Path<Object> orderBy = getPathForOrder(root, joins, criteria.getOrder().orderBy().getField());
            Order order = criteria.getOrder().orderType().isAsc() ? builder.asc(orderBy) : builder.desc(orderBy);
            query.orderBy(order);
        }

        // Build predicates for filtering
        return buildPredicates(criteria.getFilters(), root, joins, builder);
    }

    private List<Join<?, ?>> buildJoins(List<CriteriaJoin> criteriaJoins, Root<T> root, CriteriaQuery<?> query) {
        if (criteriaJoins == null || criteriaJoins.isEmpty()) {
            return Collections.emptyList();
        }

        List<Join<?, ?>> joins = new ArrayList<>();
        Set<String> processedPaths = new HashSet<>();

        for (CriteriaJoin criteriaJoin : criteriaJoins) {
            String joinPath = criteriaJoin.getJoinPath();

            if (processedPaths.contains(joinPath)) {
                continue;
            }

            From<?, ?> currentFrom = root;
            String[] pathParts = joinPath.split("\\.");

            for (int i = 0; i < pathParts.length; i++) {
                String part = pathParts[i];
                String currentFullPath = buildFullPath(currentFrom, part, i); // Helper to build full path for lookup

                Join<?, ?> existingJoin = findJoinByPath(currentFullPath, joins);

                if (existingJoin != null) {
                    currentFrom = existingJoin;
                } else {
                    JoinType jpaJoinType = criteriaJoin.getJoinTypeCriteria().isInner() ? JoinType.INNER : JoinType.LEFT;
                    currentFrom = currentFrom.join(part, jpaJoinType);
                    if (currentFrom instanceof Join<?, ?>) {
                        Join<?, ?> newJoin = (Join<?, ?>) currentFrom;
                        joins.add(newJoin);
                        joinParents.put(newJoin, currentFrom instanceof Join ? joinParents.get(currentFrom) : root);
                        // Store the parent for correct path traversal
                        //if (i > 0) { // If it's not the first part of the path, its parent is the previous `currentFrom`
                            // This logic might need refinement based on your exact `joinParents` usage.
                            // For typical scenarios, you only need to join off the root or existing joins.
                            // For nested joins, ensure `joinParents` accurately reflects the hierarchy.
                        //}
                    }
                }
            }
            processedPaths.add(joinPath);
        }
        return joins;
    }

    private String buildFullPath(From<?, ?> currentFrom, String part, int index) {
        if (index == 0) {
            return part; // First part is just the attribute name from the root
        } else if (currentFrom instanceof Join<?, ?>) {
            return ((Join<?, ?>) currentFrom).getAttribute().getName() + "." + part;
        } else {
            return part; // Should ideally be a join at this point for subsequent parts
        }
    }


    private Predicate buildPredicates(List<Filter> filters, Root<T> root, List<Join<?, ?>> joins, CriteriaBuilder builder) {
        if (filters == null || filters.isEmpty()) {
            return builder.conjunction(); // Return a "true" predicate if no filters
        }

        List<Predicate> predicates = filters.stream()
                .map(filter -> buildPredicate(filter, root, joins, builder))
                .filter(Objects::nonNull)
                .toList();

        return builder.and(predicates.toArray(new Predicate[0])); // Default to AND for top-level filters
    }

    private Predicate buildPredicate(Filter filter, Root<T> root, List<Join<?, ?>> joins, CriteriaBuilder builder) {
        if (filter instanceof SingleFilter) {
            return buildSingleFilterPredicate((SingleFilter<?>) filter, root, joins, builder);
        } else if (filter instanceof CompoundFilter) {
            return buildCompoundFilterPredicate((CompoundFilter) filter, root, joins, builder);
        }
        return null;
    }

    private Predicate buildSingleFilterPredicate(SingleFilter<?> filter, Root<T> root, List<Join<?, ?>> joins, CriteriaBuilder builder) {
        Path<?> path = buildPath(filter, root, joins);
        if (path == null) return null;

        return predicateFactory.generateSingleFilterPredicate(builder, filter, path);
    }

    private Predicate buildCompoundFilterPredicate(CompoundFilter filter, Root<T> root, List<Join<?, ?>> joins, CriteriaBuilder builder) {
        List<Predicate> nestedPredicates = filter.getFilters().stream()
                .map(nestedFilter -> buildPredicate(nestedFilter, root, joins, builder))
                .filter(Objects::nonNull)
                .toList();

        Predicate[] predicateArray = nestedPredicates.toArray(new Predicate[0]);
        return filter.getOperator() == CompoundFilterOperator.AND ? builder.and(predicateArray) : builder.or(predicateArray);

    }

    private Path<?> buildPath(SingleFilter<?> filter, Root<T> root, List<Join<?, ?>> joins) {
        return getPath(root, joins, filter.getField());
    }

    // Helper method to traverse path for both filtering and ordering
    private Path<?> getPath(Root<T> root, List<Join<?, ?>> joins, String fieldName) {
        if (!fieldName.contains(".")) {
            return root.get(fieldName);
        }

        String[] pathParts = fieldName.split("\\.");
        From<?, ?> currentFrom = root;

        for (int i = 0; i < pathParts.length; i++) {
            String part = pathParts[i];
            if (i < pathParts.length - 1) { // If it's a join part
                String joinFullPath = String.join(".", Arrays.copyOfRange(pathParts, 0, i + 1));
                Join<?, ?> join = findJoinByPath(joinFullPath, joins); // Try to find the already built join
                if (join != null) {
                    currentFrom = join;
                } else {
                    // This scenario ideally shouldn't happen if joins are built correctly upfront.
                    // If it does, it means a predicate is asking for a join that wasn't declared in CriteriaJoins.
                    // You might need to add a join here dynamically, or throw an exception.
                    throw new IllegalArgumentException("Join for path part '" + part + "' (full path: " + fieldName + ") not found. Ensure all joined paths are declared in CriteriaJoins.");
                }
            } else { // Last part is the actual attribute
                return currentFrom.get(part);
            }
        }
        return null; // Should not reach here
    }


    private Path<Object> getPathForOrder(Root<T> root, List<Join<?, ?>> joins, String fieldName) {
        return (Path<Object>) getPath(root, joins, fieldName);
    }


    // This findJoinByPath needs to be more robust, potentially tracking the full path of each join
    // The previous implementation used joinParents, but that's for traversing, not for finding a specific join by its full path from root.
    // Let's refine findJoinByPath to work with full paths.
    private Join<?, ?> findJoinByPath(String fullJoinPath, List<Join<?, ?>> joins) {
        for (Join<?, ?> join : joins) {
            String currentJoinPath = getJoinFullPath(join);
            if (fullJoinPath.equals(currentJoinPath)) {
                return join;
            }
        }
        return null;
    }

    private String getJoinFullPath(Join<?, ?> join) {
        StringBuilder path = new StringBuilder();
        From<?, ?> current = join;
        while (current instanceof Join<?, ?>) {
            Join<?, ?> currentJoin = (Join<?, ?>) current;
            path.insert(0, currentJoin.getAttribute().getName());
            if (currentJoin.getParentPath() instanceof Join<?, ?>) {
                path.insert(0, ".");
            }
            current = (From<?, ?>) currentJoin.getParentPath();
        }
        return path.toString();
    }
}
