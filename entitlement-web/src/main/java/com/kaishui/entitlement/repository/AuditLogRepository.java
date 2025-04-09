package com.kaishui.entitlement.repository;

import com.kaishui.entitlement.entity.AuditLogEntity;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends ReactiveMongoRepository<AuditLogEntity, ObjectId> {
}