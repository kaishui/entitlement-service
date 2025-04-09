package com.kaishui.entitlement.repository;

import com.kaishui.entitlement.entity.Role;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface RoleRepository extends ReactiveMongoRepository<Role, String> {
    Mono<Role> findByRoleName(String roleName);
    Mono<Boolean> existsByRoleName(String roleName);
}