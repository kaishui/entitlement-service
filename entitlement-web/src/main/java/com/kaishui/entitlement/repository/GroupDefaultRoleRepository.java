package com.kaishui.entitlement.repository;

import com.kaishui.entitlement.entity.GroupDefaultRole;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono; // Import Mono

import java.util.List;

@Repository
public interface GroupDefaultRoleRepository extends ReactiveMongoRepository<GroupDefaultRole, String> {
    Flux<GroupDefaultRole> findByGroupNameIn(List<String> groupNames);

    // Add method to find by unique groupName
    Mono<GroupDefaultRole> findByGroupName(String groupName);

    // Add method to check if a groupName exists
    Mono<Boolean> existsByGroupName(String groupName);
}