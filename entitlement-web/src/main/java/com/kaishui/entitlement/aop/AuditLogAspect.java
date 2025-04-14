package com.kaishui.entitlement.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaishui.entitlement.annotation.AuditLog;
import com.kaishui.entitlement.entity.AuditLogEntity;
import com.kaishui.entitlement.repository.AuditLogRepository;
import com.kaishui.entitlement.util.AuthorizationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.bson.Document;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final AuthorizationUtil authorizationUtil;


    @Pointcut("@annotation(com.kaishui.entitlement.annotation.AuditLog)")
    public void auditLogPointcut() {
    }

    // auditLogAround remains largely the same, just ensure it calls the updated prepareAuditDetailsAsBson
    @Around("auditLogPointcut()")
    public Object auditLogAround(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        AuditLog auditLogAnnotation = method.getAnnotation(AuditLog.class);
        String action = auditLogAnnotation.action();

        Object result = pjp.proceed();

        if (result instanceof Mono) {
            return ((Mono<?>) result)
                    .doOnSuccess(successResult -> {
                        Document details = prepareAuditDetailsAsBson(pjp, successResult, signature);
                        saveAuditLog(action, details).subscribe(
                                null,
                                error -> log.error("Error saving audit log reactively for action: {}", action, error)
                        );
                    })
                    .doOnError(error -> log.error("Original method for action '{}' failed: {}", action, error.getMessage()));

        } else if (result instanceof Flux) {
            // For Flux, consider if logging on complete is sufficient, or if you need details per item (more complex)
            return ((Flux<?>) result)
                    .doOnComplete(() -> {
                        // Details might only contain parameters here, as the result is a stream
                        Document details = prepareAuditDetailsAsBson(pjp, "[Flux Completed]", signature);
                        saveAuditLog(action, details).subscribe(
                                null,
                                error -> log.error("Error saving audit log reactively for action: {}", action, error)
                        );
                    })
                    .doOnError(error -> log.error("Original method for action '{}' failed: {}", action, error.getMessage()));
        } else {
            // Handle non-reactive results (if applicable)
            log.warn("Method annotated with @AuditLog did not return a reactive type. Logging synchronously.");
            Document details = prepareAuditDetailsAsBson(pjp, result, signature);
            saveAuditLog(action, details).block(); // Blocking call - use with caution!
            return result;
        }
    }


    private Mono<Void> saveAuditLog(String action, Document details) {
        return Mono.deferContextual(contextView -> {
                    String username = authorizationUtil.extractUsernameFromContext(contextView);
                    log.info("Preparing to save audit log for action: {}, username: {}", action, username);
                    AuditLogEntity auditLogEntity = AuditLogEntity.builder()
                            .action(action)
                            .detail(details) // Assign the BSON Document directly
                            .createdBy(username)
                            .createdDate(new Date())
                            .build();
                    return auditLogRepository.save(auditLogEntity);
                })
                .doOnError(error -> log.error("Failed during audit log save preparation for action: {}", action, error))
                .then();
    }

    private Document prepareAuditDetailsAsBson(ProceedingJoinPoint pjp, Object result, MethodSignature signature) {
        Document details = new Document();
        Object[] args = pjp.getArgs();
        String[] parameterNames = signature.getParameterNames();

        if (args.length > 0 && parameterNames.length == args.length) {
            Document parametersDoc = new Document();
            for (int i = 0; i < args.length; i++) {
                // Convert parameter to BSON-compatible format
                parametersDoc.put(parameterNames[i], convertToBsonCompatible(args[i]));
            }
            details.put("parameters", parametersDoc);
        } else if (args.length > 0) {
            // Fallback if parameter names couldn't be retrieved
            details.put("parameters", convertToBsonCompatible(List.of(args))); // Convert the list itself
        }

        // Note: For large results, you might want to omit them or store only a summary
        // if (result != null && !(result instanceof String && "[Flux Completed]".equals(result))) {
        //     details.put("result", convertToBsonCompatible(result));
        // }

        log.debug("AuditLog - Parameters doc: {}", details.get("parameters")); // Use debug level
        return details;
    }

    private Object convertToBsonCompatible(Object obj) {
        if (obj == null) {
            return null;
        }

        // Check for standard BSON types (add more as needed)
        if (obj instanceof String || obj instanceof Number || obj instanceof Boolean || obj instanceof Date || obj instanceof Document || obj instanceof byte[]) {
            return obj;
        }

        // Handle Collections (like List, Set)
        if (obj instanceof Collection<?> collection) {
            // Recursively convert elements in the collection
            return collection.stream()
                    .map(this::convertToBsonCompatible)
                    .collect(Collectors.toList());
        }

        // Handle Maps
        if (obj instanceof Map<?, ?> map) {
            Document mapDoc = new Document();
            map.forEach((key, value) -> {
                // Convert key to String if it's not already
                String keyStr = (key instanceof String) ? (String) key : String.valueOf(key);
                // Recursively convert value
                mapDoc.put(keyStr, convertToBsonCompatible(value));
            });
            return mapDoc;
        }

        // Handle reactive types (extract value if possible, or represent as string)
        // Be cautious here, blocking inside an aspect is generally bad.
        // This example just represents them as strings. A better approach might involve
        // modifying the aspect logic to handle Mono/Flux parameters differently.
        if (obj instanceof Mono) {
            return "[Mono Parameter]"; // Avoid blocking to get value
        }
        if (obj instanceof Flux) {
            return "[Flux Parameter]"; // Avoid blocking
        }


        // --- Attempt JSON serialization for complex objects ---
        try {
            // Convert the object to a Map (which Jackson can often do)
            // Map<String, Object> objectMap = objectMapper.convertValue(obj, Map.class);
            // return new Document(objectMap); // Convert the Map to a BSON Document

            // Alternative: Convert directly to JSON String
            return objectMapper.writeValueAsString(obj);

        } catch (Exception e) { // Catch broader exceptions during conversion/serialization
            log.warn("Could not convert object of type {} to BSON-compatible format for audit log: {}", obj.getClass().getName(), e.getMessage());
            // Fallback to toString() or a placeholder
            return "[Unserializable Object: " + obj.getClass().getSimpleName() + "]";
            // return obj.toString(); // Use with caution, toString() might be huge or unhelpful
        }
    }

}