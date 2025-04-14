package com.kaishui.entitlement.repository;

import com.kaishui.entitlement.entity.AuditLogEntity;
import org.bson.Document;
import org.bson.types.ObjectId;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogRepositoryTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditLogEntity log1;
    private AuditLogEntity log2;
    private String log1Id;

    @BeforeEach
    void setUp() {
        log1Id = new ObjectId().toHexString();

        // Use org.bson.Document for details1
        Document details1 = new Document()
                .append("userId", "user123")
                .append("roleId", "roleABC");

        log1 = AuditLogEntity.builder()
                .id(log1Id)
                .action("CREATE_ROLE")
                .detail(details1) // Assign the Document
                .createdBy("adminUser")
                .createdDate(new Date())
                .build();

        // Use org.bson.Document for details2
        Document details2 = new Document()
                .append("userId", "user456")
                .append("status", "inactive");

        log2 = AuditLogEntity.builder()
                .id(new ObjectId().toHexString())
                .action("DELETE_USER")
                .detail(details2) // Assign the Document
                .createdBy("system")
                .createdDate(new Date(System.currentTimeMillis() - 10000))
                .build();
    }

    @Test
    @DisplayName("Should simulate saving an audit log successfully")
    void saveAuditLog() {
        // Arrange
        when(auditLogRepository.save(any(AuditLogEntity.class))).thenReturn(Mono.just(log1));

        // Act
        Mono<AuditLogEntity> saveMono = auditLogRepository.save(log1);

        // Assert
        StepVerifier.create(saveMono)
                .assertNext(savedLog -> {
                    assertEquals(log1Id, savedLog.getId());
                    assertEquals("CREATE_ROLE", savedLog.getAction());
                    assertEquals("adminUser", savedLog.getCreatedBy());
                    assertNotNull(savedLog.getDetail());
                    // Access data using Document's get method
                    assertEquals("user123", savedLog.getDetail().getString("userId"));
                    assertEquals("roleABC", savedLog.getDetail().getString("roleId")); // Example if roleId is String
                })
                .verifyComplete();

        // Verify
        verify(auditLogRepository).save(any(AuditLogEntity.class));
    }

    @Test
    @DisplayName("Should simulate finding an audit log by ID")
    void findById() {
        // Arrange
        when(auditLogRepository.findById(log1Id)).thenReturn(Mono.just(log1));

        // Act
        Mono<AuditLogEntity> findMono = auditLogRepository.findById(log1Id);

        // Assert
        StepVerifier.create(findMono)
                .expectNext(log1)
                .verifyComplete();

        // Verify
        verify(auditLogRepository).findById(log1Id);
    }

    @Test
    @DisplayName("Should simulate returning empty Mono when finding by non-existent ID")
    void findById_NotFound() {
        // Arrange
        String nonExistentId = new ObjectId().toHexString();
        when(auditLogRepository.findById(nonExistentId)).thenReturn(Mono.empty());

        // Act
        Mono<AuditLogEntity> findMono = auditLogRepository.findById(nonExistentId);

        // Assert
        StepVerifier.create(findMono)
                .expectNextCount(0)
                .verifyComplete();

        // Verify
        verify(auditLogRepository).findById(nonExistentId);
    }

    @Test
    @DisplayName("Should simulate finding all audit logs")
    void findAllAuditLogs() {
        // Arrange
        when(auditLogRepository.findAll()).thenReturn(Flux.just(log1, log2));

        // Act
        Flux<AuditLogEntity> findAllFlux = auditLogRepository.findAll();

        // Assert
        StepVerifier.create(findAllFlux)
                .expectNext(log1)
                .expectNext(log2)
                .verifyComplete();

        // Verify
        verify(auditLogRepository).findAll();
    }

    @Test
    @DisplayName("Should simulate deleting an audit log by ID")
    void deleteById() {
        // Arrange
        when(auditLogRepository.deleteById(log1Id)).thenReturn(Mono.empty());

        // Act
        Mono<Void> deleteMono = auditLogRepository.deleteById(log1Id);

        // Assert
        StepVerifier.create(deleteMono)
                .verifyComplete();

        // Verify
        verify(auditLogRepository).deleteById(log1Id);
    }
}