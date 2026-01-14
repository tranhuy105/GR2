package com.tranhuy105.server.algorithm;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tranhuy105.server.algorithm.operator.InsertionOperator;
import com.tranhuy105.server.algorithm.operator.RemovalOperator;

import lombok.extern.slf4j.Slf4j;

/**
 * Registry for all ALNS operators.
 * Spring auto-wires all implementations via constructor injection.
 */
@Component
@Slf4j
public class OperatorRegistry {
    private final Map<String, RemovalOperator> removalOperators;
    private final Map<String, InsertionOperator> insertionOperators;

    /**
     * Spring auto-injects all RemovalOperator and InsertionOperator beans
     */
    public OperatorRegistry(
            List<RemovalOperator> removalOperatorList,
            List<InsertionOperator> insertionOperatorList
    ) {
        this.removalOperators = removalOperatorList.stream()
                .collect(Collectors.toMap(RemovalOperator::getName, Function.identity()));
        
        this.insertionOperators = insertionOperatorList.stream()
                .collect(Collectors.toMap(InsertionOperator::getName, Function.identity()));
        
        log.info("Registered {} removal operators: {}", removalOperators.size(), removalOperators.keySet());
        log.info("Registered {} insertion operators: {}", insertionOperators.size(), insertionOperators.keySet());
    }

    public RemovalOperator getRemovalOperator(String name) {
        RemovalOperator op = removalOperators.get(name);
        if (op == null) {
            throw new IllegalArgumentException("Unknown removal operator: " + name);
        }
        return op;
    }

    public InsertionOperator getInsertionOperator(String name) {
        InsertionOperator op = insertionOperators.get(name);
        if (op == null) {
            throw new IllegalArgumentException("Unknown insertion operator: " + name);
        }
        return op;
    }

    public Set<String> getRemovalOperatorNames() {
        return removalOperators.keySet();
    }

    public Set<String> getInsertionOperatorNames() {
        return insertionOperators.keySet();
    }
}
