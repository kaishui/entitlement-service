package com.kaishui.entitlement.repository;

import com.kaishui.entitlement.entity.Resource;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ResourceRepository extends ReactiveMongoRepository<Resource, String> {

    Flux<Resource> findByIsActive(boolean isActive);
    Flux<Resource> findByTypeAndIsActive(String type, boolean isActive);
    Flux<Resource> findByRegionAndIsActive(String region, boolean isActive);
    Flux<Resource> findByNameContainingIgnoreCaseAndIsActive(String name, boolean isActive);
    Flux<Resource> findByNameContainingIgnoreCase(String name); // Example without isActive filter
    Flux<Resource> findByType(String type);
    Flux<Resource> findByRegion(String region);

}