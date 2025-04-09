package com.kaishui.entitlement.repository;

import com.kaishui.entitlement.entity.AuditLogEntity;
import org.bson.types.ObjectId; // Still useful for generating unique String IDs
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class) // Use Mockito extension for JUnit 5
class AuditLogRepositoryTest {

    @Mock // Create a mock instance of the repository
    private AuditLogRepository auditLogRepository;

    private AuditLogEntity log1;
    private AuditLogEntity log2;
    private String log1Id; // ID is String, matching the repository interface

    @BeforeEach
    void setUp() {
        // Generate a unique String ID using ObjectId's hex representation
        log1Id = new ObjectId().toHexString();
        Map<String, Object> details1 = new HashMap<>();
        details1.put("userId", "user123");
        details1.put("roleId", "roleABC");

        log1 = AuditLogEntity.builder()
                .id(log1Id) // Use the String ID
                .action("CREATE_ROLE")
                .detail(details1)
                .createdBy("adminUser")
                .createdDate(new Date())
                .build();

        Map<String, Object> details2 = new HashMap<>();
        details2.put("userId", "user456");
        details2.put("status", "inactive");

        log2 = AuditLogEntity.builder()
                .id(new ObjectId().toHexString()) // Another String ID
                .action("DELETE_USER")
                .detail(details2)
                .createdBy("system")
                .createdDate(new Date(System.currentTimeMillis() - 10000))
                .build();
    }

    @Test
    @DisplayName("Should simulate saving an audit log successfully")
    void saveAuditLog() {
        // Arrange: Define mock behavior for save
        // When save is called with any AuditLogEntity, return a Mono containing our pre-built log1
        when(auditLogRepository.save(any(AuditLogEntity.class))).thenReturn(Mono.just(log1));

        // Act: Call the mocked repository method (pass the object to be "saved")
        Mono<AuditLogEntity> saveMono = auditLogRepository.save(log1);

        // Assert: Verify the result using StepVerifier
        StepVerifier.create(saveMono)
                .assertNext(savedLog -> {
                    assertEquals(log1Id, savedLog.getId()); // Check if String ID matches
                    assertEquals("CREATE_ROLE", savedLog.getAction());
                    assertEquals("adminUser", savedLog.getCreatedBy());
                    assertNotNull(savedLog.getDetail());
                    assertEquals("user123", savedLog.getDetail().get("userId"));
                })
                .verifyComplete();

        // Verify that save was called on the mock exactly once with any AuditLogEntity
        verify(auditLogRepository).save(any(AuditLogEntity.class));
    }

    @Test
    @DisplayName("Should simulate finding an audit log by ID")
    void findById() {
        // Arrange: Define mock behavior for findById with the String ID
        when(auditLogRepository.findById(log1Id)).thenReturn(Mono.just(log1));

        // Act: Call the mocked method
        Mono<AuditLogEntity> findMono = auditLogRepository.findById(log1Id);

        // Assert: Verify the result
        StepVerifier.create(findMono)
                .expectNext(log1) // Expect the mocked log1 object
                .verifyComplete();

        // Verify findById was called with the correct String ID
        verify(auditLogRepository).findById(log1Id);
    }

    @Test
    @DisplayName("Should simulate returning empty Mono when finding by non-existent ID")
    void findById_NotFound() {
        // Arrange: Define mock behavior for a non-existent String ID
        String nonExistentId = new ObjectId().toHexString();
        when(auditLogRepository.findById(nonExistentId)).thenReturn(Mono.empty());

        // Act: Call the mocked method
        Mono<AuditLogEntity> findMono = auditLogRepository.findById(nonExistentId);

        // Assert: Verify that nothing is emitted
        StepVerifier.create(findMono)
                .expectNextCount(0) // Expect no log to be emitted
                .verifyComplete();

        // Verify findById was called with the non-existent String ID
        verify(auditLogRepository).findById(nonExistentId);
    }

    @Test
    @DisplayName("Should simulate finding all audit logs")
    void findAllAuditLogs() {
        // Arrange: Define mock behavior for findAll
        when(auditLogRepository.findAll()).thenReturn(Flux.just(log1, log2));

        // Act: Call the mocked method
        Flux<AuditLogEntity> findAllFlux = auditLogRepository.findAll();

        // Assert: Verify the emitted logs
        StepVerifier.create(findAllFlux)
                .expectNext(log1)
                .expectNext(log2)
                .verifyComplete();

        // Verify findAll was called
        verify(auditLogRepository).findAll();
    }

    @Test
    @DisplayName("Should simulate deleting an audit log by ID")
    void deleteById() {
        // Arrange: Define mock behavior for deleteById (returns Mono<Void>) with the String ID
        when(auditLogRepository.deleteById(log1Id)).thenReturn(Mono.empty()); // Return an empty Mono to signify completion

        // Act: Call the mocked method
        Mono<Void> deleteMono = auditLogRepository.deleteById(log1Id);

        // Assert: Verify that the Mono completes successfully
        StepVerifier.create(deleteMono)
                .verifyComplete();

        // Verify deleteById was called with the correct String ID
        verify(auditLogRepository).deleteById(log1Id);
    }
}