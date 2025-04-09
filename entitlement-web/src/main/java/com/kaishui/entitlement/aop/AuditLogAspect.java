package com.kaishui.entitlement.aop;

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
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogRepository auditLogRepository;

    @Pointcut("@annotation(com.kaishui.entitlement.annotation.AuditLog)")
    public void auditLogPointcut() {
    }

    // Use @Around advice
    @Around("auditLogPointcut()")
    public Object auditLogAround(ProceedingJoinPoint pjp) throws Throwable { // Must accept ProceedingJoinPoint
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        AuditLog auditLogAnnotation = method.getAnnotation(AuditLog.class);
        String action = auditLogAnnotation.action();

        // Proceed with the original method execution
        Object result = pjp.proceed(); // This returns the Mono/Flux or other object from the original method

        // Check if the result is a reactive type (Mono or Flux)
        if (result instanceof Mono) {
            // If it's a Mono, attach the logging logic
            return ((Mono<?>) result)
                    .doOnSuccess(successResult -> { // Log only on successful completion
                        // Prepare details *after* successful execution
                        Map<String, Object> details = prepareAuditDetails(pjp, successResult, signature);
                        // Perform the logging within doOnSuccess, using deferContextual
                        saveAuditLog(action, details).subscribe( // Subscribe here to trigger save
                                null, // No value expected on success
                                error -> log.error("Error saving audit log reactively for action: {}", action, error) // Log errors from saving
                        );
                    })
                    // Optionally add doOnError if you want to log failures of the main operation
                    .doOnError(error -> log.error("Original method for action '{}' failed: {}", action, error.getMessage()));

        } else if (result instanceof Flux) {
            // If it's a Flux, attach the logging logic (logging usually happens on completion)
            // This example logs *after* the Flux completes successfully.
            // Logging individual elements might require different logic (e.g., doOnNext).
            return ((Flux<?>) result)
                    .doOnComplete(() -> { // Log when the Flux completes successfully
                        // Prepare details (result here might be less meaningful for a Flux, maybe log count?)
                        Map<String, Object> details = prepareAuditDetails(pjp, "[Flux Completed]", signature); // Represent Flux completion
                        saveAuditLog(action, details).subscribe(
                                null,
                                error -> log.error("Error saving audit log reactively for action: {}", action, error)
                        );
                    })
                    .doOnError(error -> log.error("Original method for action '{}' failed: {}", action, error.getMessage()));
        } else {
            // If the result is not reactive, log synchronously (less common in WebFlux)
            // This part might need adjustment based on whether non-reactive methods are annotated
            log.warn("Method annotated with @AuditLog did not return a reactive type. Logging synchronously.");
            Map<String, Object> details = prepareAuditDetails(pjp, result, signature);
            // Synchronous logging might block, consider if this is acceptable
            saveAuditLog(action, details).block(); // Blocking call - use with caution!
            return result;
        }
    }

    // Renamed from auditLog to saveAuditLog for clarity
    private Mono<Void> saveAuditLog(String action, Map<String, Object> details) {
        return Mono.deferContextual(contextView -> {
                    String username = AuthorizationUtil.extractUsernameFromContext(contextView);
                    log.info("Preparing to save audit log for action: {}, username: {}", action, username);
                    AuditLogEntity auditLogEntity = AuditLogEntity.builder()
                            .action(action)
                            .detail(details)
                            .createdBy(username)
                            .createdDate(new Date())
                            .build();
                    // The save operation itself is reactive
                    return auditLogRepository.save(auditLogEntity);
                })
                .doOnError(error -> log.error("Failed during audit log save preparation for action: {}", action, error))
                .then(); // Convert Mono<AuditLogEntity> to Mono<Void>
    }

    // Updated prepareAuditDetails to accept ProceedingJoinPoint
    private Map<String, Object> prepareAuditDetails(ProceedingJoinPoint pjp, Object result, MethodSignature signature) {
        Map<String, Object> details = new HashMap<>();
        Object[] args = pjp.getArgs(); // Get args from ProceedingJoinPoint
        String[] parameterNames = signature.getParameterNames();

        // Add parameters map directly
        if (args.length > 0 && parameterNames.length == args.length) {
            Map<String, Object> parametersMap = new HashMap<>();
            for (int i = 0; i < args.length; i++) {
                // Consider filtering sensitive parameters (e.g., passwords)
                // Ensure args[i] is serializable by Jackson/BSON codec
                parametersMap.put(parameterNames[i], args[i]);
            }
            details.put("parameters", parametersMap); // Store the map directly
        } else if (args.length > 0) {
            // Fallback: Store the args array directly (less structured)
            // Ensure elements in args are serializable
            details.put("parameters", args);
        }
        log.info("AditLog - Parameters map: {}", details.get("parameters"));

//        // Add result object directly if not null
//        if (result != null) {
//            // Ensure the result object is serializable
//            // Be cautious about storing very large result objects
//            details.put("result", result); // Store the result object directly
//        }

        return details;
    }
}