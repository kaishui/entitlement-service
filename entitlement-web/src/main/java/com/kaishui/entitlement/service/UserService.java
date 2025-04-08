package com.kaishui.entitlement.service;

import com.kaishui.entitlement.entity.User;
import com.kaishui.entitlement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public Flux<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Mono<User> getUserById(ObjectId id) {
        return userRepository.findById(id);
    }

    public Mono<User> createUser(User user) {
        log.info("Creating user: {}", user);
        return userRepository.save(user);
    }

    public Mono<User> updateUser(User user) {
        return userRepository.save(user);
    }

    public Mono<Void> deleteUser(ObjectId id) {
        return userRepository.deleteById(id);
    }
}