package com.example.demo.common.infrastructure.criteria;

import com.example.demo.common.domain.criteria.SingleFilter;
import com.example.demo.common.domain.criteria.SingleFilterOperator;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@FunctionalInterface
interface TriFunction<T, U, V, R> {
    R apply(T t, U u, V v);
}

@Component
public class PredicateFactory {

    private final Map<Class<?>, TriFunction<CriteriaBuilder, Path<?>, Number, Predicate>> greaterThanHandlers = new HashMap<>();
    private final Map<Class<?>, TriFunction<CriteriaBuilder, Path<?>, Number, Predicate>> lessThanHandlers = new HashMap<>();
    private final Map<SingleFilterOperator, TriFunction<CriteriaBuilder, SingleFilter<?>, Path<?>, Predicate>> predicateTransformers = new HashMap<>();

    private void initializeHandlers() {
        predicateTransformers.put(SingleFilterOperator.EQUAL, this::equalsPredicateTransformer);
        predicateTransformers.put(SingleFilterOperator.NOT_EQUAL, this::notEqualsPredicateTransformer);
        predicateTransformers.put(SingleFilterOperator.GT, this::greaterThanPredicateTransformer);
        predicateTransformers.put(SingleFilterOperator.LT, this::lowerThanPredicateTransformer);
        predicateTransformers.put(SingleFilterOperator.CONTAINS, this::containsPredicateTransformer);
        predicateTransformers.put(SingleFilterOperator.NOT_CONTAINS, this::notContainsPredicateTransformer);
        predicateTransformers.put(SingleFilterOperator.IN, this::inPredicateTransformer);
    }

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

    public PredicateFactory() {
        initializeLessThanHandlers();
        initializeGreaterThanHandlers();
        initializeHandlers();
    }

    public Predicate generateSingleFilterPredicate(CriteriaBuilder builder, SingleFilter<?> filter, Path<?> path){

        TriFunction<CriteriaBuilder, SingleFilter<?>, Path<?>, Predicate> transformer = predicateTransformers.get(filter.getOperator());
        if (transformer == null) {
            throw new IllegalArgumentException("Unsupported operator: " + filter.getOperator()); // Or handle gracefully
        }

        return transformer.apply(builder, filter, path);
    }

    private Predicate equalsPredicateTransformer(CriteriaBuilder builder, SingleFilter<?> filter, Path<?> path) {
        return builder.equal(path, filter.getValue());
    }

    private Predicate notEqualsPredicateTransformer(CriteriaBuilder builder, SingleFilter<?> filter, Path<?> path) {
        return builder.notEqual(path, filter.getValue());
    }

    private Predicate greaterThanPredicateTransformer(CriteriaBuilder builder, SingleFilter<?> filter, Path<?> path) {
        Object filterValue = filter.getValue();
        Class<?> pathJavaType = path.getJavaType();

        if (!(filterValue instanceof Number)) throw new RuntimeException("Error, filter value must be Number");

        // Get the specific handler for the detected type
        TriFunction<CriteriaBuilder, Path<?>, Number, Predicate> handler = greaterThanHandlers.get(pathJavaType);

        if (handler == null) {
            throw new IllegalArgumentException("Error: Unsupported numeric type for 'greaterThan' predicate on field with type: " + pathJavaType.getName());
        }

        // Apply the handler to create the predicate
        return handler.apply(builder, path, (Number) filterValue);

    }

    private Predicate lowerThanPredicateTransformer(CriteriaBuilder builder, SingleFilter<?> filter, Path<?> path) {
        Object filterValue = filter.getValue();
        Class<?> pathJavaType = path.getJavaType();

        if (!(filterValue instanceof Number)) throw new RuntimeException("Error, filter value must be Number");

        // Get the specific handler for the detected type
        TriFunction<CriteriaBuilder, Path<?>, Number, Predicate> handler = lessThanHandlers.get(pathJavaType);

        if (handler == null) {
            throw new IllegalArgumentException("Error: Unsupported numeric type for 'greaterThan' predicate on field with type: " + pathJavaType.getName());
        }

        // Apply the handler to create the predicate
        return handler.apply(builder, path, (Number) filterValue);
    }

    private Predicate containsPredicateTransformer(CriteriaBuilder builder, SingleFilter<?> filter, Path<?> path) {
        return builder.like(path.as(String.class), String.format("%%%s%%", filter.getValue()));
    }

    private Predicate notContainsPredicateTransformer(CriteriaBuilder builder, SingleFilter<?> filter, Path<?> path) {
        return builder.notLike(path.as(String.class), String.format("%%%s%%", filter.getValue()));
    }

    private Predicate andPredicateTransformer(CriteriaBuilder builder, Predicate... predicates) {
        return builder.and(predicates);
    }

    private Predicate orPredicateTransformer(CriteriaBuilder builder, Predicate... predicates) {
        return builder.or(predicates);
    }

    private Predicate inPredicateTransformer(CriteriaBuilder builder, SingleFilter<?> filter, Path<?> path) {
        String[] values = ((String) filter.getValue()).split(",");
        List<String> valueList = Arrays.asList(values);

        return path.in(valueList);
    }

    private Predicate isNullTransformer(CriteriaBuilder builder, SingleFilter<?> filter, Path<?> path) {
        return builder.isNull(path);
    }

    private Predicate buildGreaterThanInteger(CriteriaBuilder builder, Path<?> path, Number filterValue) {
        return builder.greaterThan(path.as(Integer.class), filterValue.intValue());
    }

    private Predicate buildGreaterThanLong(CriteriaBuilder builder, Path<?> path, Number filterValue) {
        return builder.greaterThan(path.as(Long.class), filterValue.longValue());
    }

    private Predicate buildGreaterThanDouble(CriteriaBuilder builder, Path<?> path, Number filterValue) {
        return builder.greaterThan(path.as(Double.class), filterValue.doubleValue());
    }

    private Predicate buildGreaterThanFloat(CriteriaBuilder builder, Path<?> path, Number filterValue) {
        return builder.greaterThan(path.as(Float.class), filterValue.floatValue());
    }

    private Predicate buildGreaterThanShort(CriteriaBuilder builder, Path<?> path, Number filterValue) {
        return builder.greaterThan(path.as(Short.class), filterValue.shortValue());
    }

    private Predicate buildGreaterThanByte(CriteriaBuilder builder, Path<?> path, Number filterValue) {
        return builder.greaterThan(path.as(Byte.class), filterValue.byteValue());
    }

    private Predicate buildLessThanInteger(CriteriaBuilder builder, Path<?> path, Number filterValue) {
        return builder.lessThan(path.as(Integer.class), filterValue.intValue());
    }

    private Predicate buildLessThanLong(CriteriaBuilder builder, Path<?> path, Number filterValue) {
        return builder.lessThan(path.as(Long.class), filterValue.longValue());
    }

    private Predicate buildLessThanDouble(CriteriaBuilder builder, Path<?> path, Number filterValue) {
        return builder.lessThan(path.as(Double.class), filterValue.doubleValue());
    }

    private Predicate buildLessThanFloat(CriteriaBuilder builder, Path<?> path, Number filterValue) {
        return builder.lessThan(path.as(Float.class), filterValue.floatValue());
    }

    private Predicate buildLessThanShort(CriteriaBuilder builder, Path<?> path, Number filterValue) {
        return builder.lessThan(path.as(Short.class), filterValue.shortValue());
    }

    private Predicate buildLessThanByte(CriteriaBuilder builder, Path<?> path, Number filterValue) {
        return builder.lessThan(path.as(Byte.class), filterValue.byteValue());
    }
}