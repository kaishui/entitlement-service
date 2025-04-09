package com.kaishui.entitlement.repository;

import com.kaishui.entitlement.entity.GroupDefaultRole;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.List;

@Repository
public interface GroupDefaultRoleRepository extends ReactiveMongoRepository<GroupDefaultRole, String> {
    Flux<GroupDefaultRole> findByGroupNameIn(List<String> groupNames);
}