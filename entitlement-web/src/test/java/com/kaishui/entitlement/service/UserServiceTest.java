package com.kaishui.entitlement.service;

import com.kaishui.entitlement.entity.User;
import com.kaishui.entitlement.repository.UserRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(new ObjectId());
        user.setUsername("testuser");
        user.setStaffId("12345");
        user.setEmail("test@example.com");
        user.setCreatedDate(new Date());
        user.setLastModifiedDate(new Date());
        user.setAdGroups(List.of("AD_Users"));
    }

    @Test
    void getAllUsers() {
        when(userRepository.findAll()).thenReturn(Flux.just(user));

        Flux<User> found = userService.getAllUsers();

        StepVerifier.create(found)
                .expectNext(user)
                .verifyComplete();

        verify(userRepository).findAll();
    }

    @Test
    void getUserById() {
        when(userRepository.findById(user.getId())).thenReturn(Mono.just(user));

        Mono<User> found = userService.getUserById(user.getId());

        StepVerifier.create(found)
                .expectNext(user)
                .verifyComplete();

        verify(userRepository).findById(user.getId());
    }

    @Test
    void createUser() {
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));

        Mono<User> created = userService.createUser(user);

        StepVerifier.create(created)
                .expectNext(user)
                .verifyComplete();

        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_duplicateStaffId() {
        when(userRepository.save(any(User.class))).thenReturn(Mono.error(new DuplicateKeyException("Duplicate StaffId")));

        Mono<User> created = userService.createUser(user);

        StepVerifier.create(created)
                .expectError(DuplicateKeyException.class)
                .verify();

        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser() {
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));

        Mono<User> updated = userService.updateUser(user);

        StepVerifier.create(updated)
                .expectNext(user)
                .verifyComplete();

        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_duplicateStaffId() {
        when(userRepository.save(any(User.class))).thenReturn(Mono.error(new DuplicateKeyException("Duplicate StaffId")));

        Mono<User> updated = userService.updateUser(user);

        StepVerifier.create(updated)
                .expectError(DuplicateKeyException.class)
                .verify();

        verify(userRepository).save(any(User.class));
    }

    @Test
    void deleteUser() {
        when(userRepository.deleteById(user.getId())).thenReturn(Mono.empty());

        Mono<Void> deleted = userService.deleteUser(user.getId());

        StepVerifier.create(deleted)
                .verifyComplete();

        verify(userRepository).deleteById(user.getId());
    }
}