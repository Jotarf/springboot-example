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
    private final Map<Join<?, ?>, From<?, ?>> joinParents = new HashMap<>();
    private final Map<Class<?>, BiFunction<Path<?>, Number, Predicate>> greaterThanHandlers = new HashMap<>();
    private final Map<Class<?>, BiFunction<Path<?>, Number, Predicate>> lessThanHandlers = new HashMap<>();

    private final HashMap<SingleFilterOperator, BiFunction<SingleFilter<?>, Path<?>, Predicate>> predicateTransformers = new HashMap<>() {{
        put(SingleFilterOperator.EQUAL, HibernateCriteriaConverter.this::equalsPredicateTransformer);
        put(SingleFilterOperator.NOT_EQUAL, HibernateCriteriaConverter.this::notEqualsPredicateTransformer);
        put(SingleFilterOperator.GT, HibernateCriteriaConverter.this::greaterThanPredicateTransformer);
        put(SingleFilterOperator.LT, HibernateCriteriaConverter.this::lowerThanPredicateTransformer);
        put(SingleFilterOperator.CONTAINS, HibernateCriteriaConverter.this::containsPredicateTransformer);
        put(SingleFilterOperator.NOT_CONTAINS, HibernateCriteriaConverter.this::notContainsPredicateTransformer);
        put(SingleFilterOperator.IN, HibernateCriteriaConverter.this::inPredicateTransformer);
    }};

    private void initializeGreaterThanHandlers() {
        // Populate the map with handlers for each numeric type
        // The BiFunction takes Path and Number, and returns a Predicate
        greaterThanHandlers.put(Integer.class, this::buildGreaterThanInteger);
        greaterThanHandlers.put(int.class, this::buildGreaterThanInteger); // Handle primitive int
        greaterThanHandlers.put(Long.class, this::buildGreaterThanLong);
        greaterThanHandlers.put(long.class, this::buildGreaterThanLong); // Handle primitive long
        greaterThanHandlers.put(Double.class, this::buildGreaterThanDouble);
        greaterThanHandlers.put(double.class, this::buildGreaterThanDouble); // Handle primitive double
        greaterThanHandlers.put(Float.class, this::buildGreaterThanFloat);
        greaterThanHandlers.put(float.class, this::buildGreaterThanFloat);
        greaterThanHandlers.put(Short.class, this::buildGreaterThanShort);
        greaterThanHandlers.put(short.class, this::buildGreaterThanShort);
        greaterThanHandlers.put(Byte.class, this::buildGreaterThanByte);
        greaterThanHandlers.put(byte.class, this::buildGreaterThanByte);
    }

    private void initializeLessThanHandlers() {
        // Populate the map with handlers for each numeric type
        // The BiFunction takes Path and Number, and returns a Predicate
        lessThanHandlers.put(Integer.class, this::buildLessThanInteger);
        lessThanHandlers.put(int.class, this::buildLessThanInteger); // Handle primitive int
        lessThanHandlers.put(Long.class, this::buildLessThanLong);
        lessThanHandlers.put(long.class, this::buildLessThanLong); // Handle primitive long
        lessThanHandlers.put(Double.class, this::buildLessThanDouble);
        lessThanHandlers.put(double.class, this::buildLessThanDouble); // Handle primitive double
        lessThanHandlers.put(Float.class, this::buildLessThanFloat);
        lessThanHandlers.put(float.class, this::buildLessThanFloat);
        lessThanHandlers.put(Short.class, this::buildLessThanShort);
        lessThanHandlers.put(short.class, this::buildLessThanShort);
        lessThanHandlers.put(Byte.class, this::buildLessThanByte);
        lessThanHandlers.put(byte.class, this::buildLessThanByte);
    }

    public HibernateCriteriaConverter(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.builder = entityManager.getCriteriaBuilder();
        initializeGreaterThanHandlers();
        initializeLessThanHandlers();
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

        BiFunction<SingleFilter<?>, Path<?>, Predicate> transformer = predicateTransformers.get(filter.getOperator());
        if (transformer == null) {
            throw new IllegalArgumentException("Unsupported operator: " + filter.getOperator()); // Or handle gracefully
        }
        return transformer.apply(filter, path);
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

    private Predicate equalsPredicateTransformer(SingleFilter<?> filter, Path<?> path) {
        return builder.equal(path, filter.getValue());
    }

    private Predicate notEqualsPredicateTransformer(SingleFilter<?> filter, Path<?> path) {
        return builder.notEqual(path, filter.getValue());
    }

    private Predicate greaterThanPredicateTransformer(SingleFilter<?> filter, Path<?> path) {
        Object filterValue = filter.getValue();
        Class<?> pathJavaType = path.getJavaType();

        if (!(filterValue instanceof Number)) throw new RuntimeException("Error, filter value must be Number");

        // Get the specific handler for the detected type
        BiFunction<Path<?>, Number, Predicate> handler = greaterThanHandlers.get(pathJavaType);

        if (handler == null) {
            throw new IllegalArgumentException("Error: Unsupported numeric type for 'greaterThan' predicate on field with type: " + pathJavaType.getName());
        }

        // Apply the handler to create the predicate
        return handler.apply(path, (Number) filterValue);

    }

    private Predicate lowerThanPredicateTransformer(SingleFilter<?> filter, Path<?> path) {
        Object filterValue = filter.getValue();
        Class<?> pathJavaType = path.getJavaType();

        if (!(filterValue instanceof Number)) throw new RuntimeException("Error, filter value must be Number");

        // Get the specific handler for the detected type
        BiFunction<Path<?>, Number, Predicate> handler = lessThanHandlers.get(pathJavaType);

        if (handler == null) {
            throw new IllegalArgumentException("Error: Unsupported numeric type for 'greaterThan' predicate on field with type: " + pathJavaType.getName());
        }

        // Apply the handler to create the predicate
        return handler.apply(path, (Number) filterValue);
    }

    private Predicate containsPredicateTransformer(SingleFilter<?> filter, Path<?> path) {
        return builder.like(path.as(String.class), String.format("%%%s%%", filter.getValue()));
    }

    private Predicate notContainsPredicateTransformer(SingleFilter<?> filter, Path<?> path) {
        return builder.notLike(path.as(String.class), String.format("%%%s%%", filter.getValue()));
    }

    private Predicate andPredicateTransformer(Predicate... predicates) {
        return builder.and(predicates);
    }

    private Predicate orPredicateTransformer(Predicate... predicates) {
        return builder.or(predicates);
    }

    private Predicate inPredicateTransformer(SingleFilter<?> filter, Path<?> path) {
        String[] values = ((String) filter.getValue()).split(",");
        List<String> valueList = Arrays.asList(values);

        return path.in(valueList);
    }

    private Predicate isNullTransformer(SingleFilter<?> filter, Path<?> path) {
        return builder.isNull(path);
    }

    private Predicate buildGreaterThanInteger(Path<?> path, Number filterValue) {
        return builder.greaterThan(path.as(Integer.class), filterValue.intValue());
    }

    private Predicate buildGreaterThanLong(Path<?> path, Number filterValue) {
        return builder.greaterThan(path.as(Long.class), filterValue.longValue());
    }

    private Predicate buildGreaterThanDouble(Path<?> path, Number filterValue) {
        return builder.greaterThan(path.as(Double.class), filterValue.doubleValue());
    }

    private Predicate buildGreaterThanFloat(Path<?> path, Number filterValue) {
        return builder.greaterThan(path.as(Float.class), filterValue.floatValue());
    }

    private Predicate buildGreaterThanShort(Path<?> path, Number filterValue) {
        return builder.greaterThan(path.as(Short.class), filterValue.shortValue());
    }

    private Predicate buildGreaterThanByte(Path<?> path, Number filterValue) {
        return builder.greaterThan(path.as(Byte.class), filterValue.byteValue());
    }

    private Predicate buildLessThanInteger(Path<?> path, Number filterValue) {
        return builder.lessThan(path.as(Integer.class), filterValue.intValue());
    }

    private Predicate buildLessThanLong(Path<?> path, Number filterValue) {
        return builder.lessThan(path.as(Long.class), filterValue.longValue());
    }

    private Predicate buildLessThanDouble(Path<?> path, Number filterValue) {
        return builder.lessThan(path.as(Double.class), filterValue.doubleValue());
    }

    private Predicate buildLessThanFloat(Path<?> path, Number filterValue) {
        return builder.lessThan(path.as(Float.class), filterValue.floatValue());
    }

    private Predicate buildLessThanShort(Path<?> path, Number filterValue) {
        return builder.lessThan(path.as(Short.class), filterValue.shortValue());
    }

    private Predicate buildLessThanByte(Path<?> path, Number filterValue) {
        return builder.lessThan(path.as(Byte.class), filterValue.byteValue());
    }
}
