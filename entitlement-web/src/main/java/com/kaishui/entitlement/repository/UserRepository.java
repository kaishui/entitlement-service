package com.kaishui.entitlement.repository;

import com.kaishui.entitlement.entity.User;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends ReactiveMongoRepository<User, ObjectId> {
    Mono<User> findByUsername(String username);

    Mono<User> findByStaffId(String staffId);

//    @Aggregation(pipeline = {
//            "{ '$group': { '_id': '$department', 'count': { '$sum': 1 } } }",
//            "{ '$project': { '_id': 0, 'department': '$_id', 'count': 1 } }"
//    })
//    Flux<DepartmentUserCount> countUsersByDepartment();
}