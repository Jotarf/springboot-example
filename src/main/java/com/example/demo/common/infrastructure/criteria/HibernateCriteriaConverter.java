package com.example.demo.common.infrastructure.criteria;

import com.example.demo.common.domain.criteria.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jakarta.persistence.criteria.Order;
import java.util.*;
import java.util.function.BiFunction;

@Component
public class HibernateCriteriaConverter<T> {
    private final CriteriaBuilder builder;
    private final EntityManager entityManager;
    private final PredicateFactory predicateFactory;
    private final Map<Join<?, ?>, From<?, ?>> joinParents = new HashMap<>();

    public HibernateCriteriaConverter(EntityManager entityManager, PredicateFactory predicateFactory) {
        this.entityManager = entityManager;
        this.builder = entityManager.getCriteriaBuilder();
        this.predicateFactory = predicateFactory;
    }

    public TypedQuery<T> convert(Criteria criteria, Class<T> aggregateClass) {

        CriteriaQuery<T> hibernateCriteria = builder.createQuery(aggregateClass);
        Root<T> root = hibernateCriteria.from(aggregateClass);

        List<Join<?, ?>> joins = buildJoins(criteria.getCriteriaJoins(), root);
        hibernateCriteria.where(buildPredicates(criteria.getFilters(), root, joins));

        if (criteria.getOrder().hasOrder()) {
            Path<Object> orderBy = root.get(criteria.getOrder().orderBy().getField());
            Order order = criteria.getOrder().orderType().isAsc() ? builder.asc(orderBy) : builder.desc(orderBy);
            hibernateCriteria.orderBy(order);
        }

        TypedQuery<T> query = entityManager.createQuery(hibernateCriteria);

        if (criteria.getOffset() != null) {
            query.setFirstResult(criteria.getOffset());
        }

        if (criteria.getLimit() != null) {
            query.setMaxResults(criteria.getLimit());
        }

        return query;
    }

    private List<Join<?, ?>> buildJoins(List<CriteriaJoin> criteriaJoins, Root<T> root) {
        if (criteriaJoins == null || criteriaJoins.isEmpty()) {
            return Collections.emptyList();
        }

        List<Join<?, ?>> joins = new ArrayList<>();
        Set<String> processedPaths = new HashSet<>();

        for (CriteriaJoin criteriaJoin : criteriaJoins) {
            String joinPath = criteriaJoin.getJoinPath();

            if (processedPaths.contains(joinPath)) {
                continue; // Skip if already processed
            }

            From<?, ?> currentFrom = root;
            String[] pathParts = joinPath.split("\\.");

            for (int i = 0; i < pathParts.length; i++) {
                String part = pathParts[i];
                String currentPath = (i == 0 ? "" : (currentFrom instanceof Join ? ((Join<?, ?>) currentFrom).getAttribute().getName() : "") + ".") + part;

                Join<?, ?> existingJoin = findJoinByPath(currentPath, joins);

                if (existingJoin != null) {
                    currentFrom = existingJoin;
                } else {
                    currentFrom = currentFrom.join(part, criteriaJoin.getJoinTypeCriteria().isInner() ? JoinType.INNER : JoinType.LEFT);
                    if (currentFrom instanceof Join<?, ?>) {
                        Join<?, ?> newJoin = (Join<?, ?>) currentFrom;
                        joins.add(newJoin);
                        joinParents.put(newJoin, currentFrom instanceof Join ? joinParents.get(currentFrom) : root); // Store the parent HERE
                    }
                }
            }
            processedPaths.add(joinPath); // Mark the entire path as processed
        }
        return joins;
    }

    private Predicate[] buildPredicates(List<Filter> filters, Root<T> root, List<Join<?, ?>> joins) {
        if (filters == null || filters.isEmpty()) {
            return new Predicate[0]; // Return empty array if no filters
        }

        return filters.stream()
                .map(filter -> buildPredicate(filter, root, joins))
                .filter(Objects::nonNull) // Handle null predicates (e.g., unsupported filter types)
                .toArray(Predicate[]::new);
    }

    private Predicate buildPredicate(Filter filter, Root<T> root, List<Join<?, ?>> joins) {
        if (filter instanceof SingleFilter) {
            return buildSingleFilterPredicate((SingleFilter<?>) filter, root, joins);
        } else if (filter instanceof CompoundFilter) {
            return buildCompoundFilterPredicate((CompoundFilter) filter, root, joins);
        }
        return null; // Handle unsupported filter types gracefully
    }

    private Predicate buildSingleFilterPredicate(SingleFilter<?> filter, Root<T> root, List<Join<?, ?>> joins) {
        Path<?> path = buildPath(filter, root, joins);
        if (path == null) return null; //Path not found

        return predicateFactory.generateSingleFilterPredicate(builder, filter, path);
    }

    private Predicate buildCompoundFilterPredicate(CompoundFilter filter, Root<T> root, List<Join<?, ?>> joins) {
        Predicate[] predicates = buildPredicates(filter.getFilters(), root, joins);
        return filter.getOperator() == CompoundFilterOperator.AND ? builder.and(predicates) : builder.or(predicates);
    }

    private Path<?> buildPath(SingleFilter<?> filter, Root<T> root, List<Join<?, ?>> joins) {
        if (!filter.getField().contains(".")) {
            return root.get(filter.getField());
        }

        String[] pathParts = filter.getField().split("\\.");
        Path<?> currentPath = root;

        for (int i = 0; i < pathParts.length; i++) {
            String part = pathParts[i];

            if (i == 0) { // First part is the join name
                Join<?, ?> join = findJoin(part, joins, root);
                if (join == null) {
                    throw new IllegalArgumentException("Join not found for path: " + filter.getField());
                }
                currentPath = join; // Update the from
            } else { // Subsequent parts are attribute names
                currentPath = currentPath.get(part);
            }
        }
        return currentPath;
    }

    private Join<?, ?> findJoin(String joinPath, List<Join<?, ?>> joins, Root<?> root) {
        // First check in joins list
        for (Join<?, ?> join : joins) {
            if (join.getAttribute().getName().equals(joinPath)) {
                return join;
            }
        }

        // If not found in the list, try to get it from root
        if (root.getJavaType().getSimpleName().equals(joinPath)) {
            return (Join<?, ?>) root;
        }

        return null;
    }

    private Join<?, ?> findJoinByPath(String joinPath, List<Join<?, ?>> joins) {

        for (Join<?, ?> join : joins) {
            String existingJoinPath = "";
            From<?, ?> from = join;

            while (from != null) { // Traverse up using stored parents
                if (from instanceof Join<?, ?>) {
                    existingJoinPath = ((Join<?, ?>) from).getAttribute().getName() + "." + existingJoinPath;
                    from = joinParents.get(from); // Get the stored parent
                } else {
                    break; // Reached the root
                }
            }

            if (!existingJoinPath.isEmpty()) {
                existingJoinPath = existingJoinPath.substring(0, existingJoinPath.length() - 1); // Remove trailing dot
            }

            if (joinPath.equals(existingJoinPath)) {
                return join;
            }
        }
        return null;
    }
}
