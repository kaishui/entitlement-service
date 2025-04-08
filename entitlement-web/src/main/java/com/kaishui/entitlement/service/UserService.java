package com.kaishui.entitlement.service;

import com.kaishui.entitlement.entity.GroupDefaultRole;
import com.kaishui.entitlement.entity.User;
import com.kaishui.entitlement.exception.CommonException;
import com.kaishui.entitlement.repository.GroupDefaultRoleRepository;
import com.kaishui.entitlement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final GroupDefaultRoleRepository groupDefaultRoleRepository;

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

    public Mono<User> processFirstLogin(User user) {
        if (!user.isFirstLogin()) {
            return Mono.just(user); // Not first login, do nothing
        }

        return groupDefaultRoleRepository.findByGroupNameIn(user.getAdGroups())
                .map(GroupDefaultRole::getRoleIds)
                .collectList()
                .map(lists -> lists.stream().flatMap(List::stream).distinct().toList())
                .flatMap(roleIds -> {
                    user.setRoleIds(roleIds);
                    user.setFirstLogin(false);
                    return userRepository.save(user);
                });
    }

    @Transactional
    public Mono<User> insertOrUpdateUser(User user) {
        log.info("Inserting or updating user: {}", user);
        return userRepository.findByStaffId(user.getStaffId())
                .flatMap(existingUser -> {
                    if (!existingUser.isActive()) {
                        return Mono.error(new CommonException("Cannot update inactive user with staffId: " + user.getStaffId()));
                    }
                    // Update existing user's fields
                    User updatedUser = User.builder()
                            .id(existingUser.getId())
                            .username(user.getUsername())
                            .staffId(existingUser.getStaffId())
                            .email(user.getEmail())
                            .department(user.getDepartment())
                            .functionalManager(user.getFunctionalManager())
                            .entityManager(user.getEntityManager())
                            .jobTitle(user.getJobTitle())
                            .isActive(existingUser.isActive())
                            .createdBy(existingUser.getCreatedBy())
                            .updatedBy(user.getUpdatedBy())
                            .createdDate(existingUser.getCreatedDate())
                            .lastModifiedDate(user.getLastModifiedDate())
                            .adGroups(user.getAdGroups())
                            .roleIds(existingUser.getRoleIds())
                            .isFirstLogin(existingUser.isFirstLogin())
                            .build();
                    return userRepository.save(existingUser);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // Insert new user
                    return userRepository.save(user);
                }));
    }
}