package com.kaishui.entitlement.repository;

import com.kaishui.entitlement.entity.Role;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public interface RoleRepository extends ReactiveMongoRepository<Role, String> {
    Mono<Role> findByRoleName(String roleName);
    Mono<Boolean> existsByRoleName(String roleName);

    Flux<Role> findAllByIdAndIsActive(List<String> roleIds, boolean isActive);
    Flux<Role> findAllByUserCaseAndIsActive(String userCase, boolean isActive);
    @Aggregation(pipeline = {
            "{ $match: { '_id': { $in: ?0 }, 'userCase': ?1, 'isActive': ?2 } }"
    })
    Flux<Role> findAllByIdsAndUserCaseAndIsActive(List<String> roleIds, String userCase, boolean isActive);}