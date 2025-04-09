package com.kaishui.entitlement.repository;

import com.kaishui.entitlement.entity.User;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRepositoryTest {

    @Mock
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(new ObjectId().toHexString());
        user.setUsername("testuser");
        user.setStaffId("12345");
        user.setEmail("test@example.com");
        user.setCreatedDate(new Date());
        user.setLastModifiedDate(new Date());
        user.setAdGroups(List.of("AD_Users"));
    }

    @Test
    void findAll() {
        when(userRepository.findAll()).thenReturn(Flux.just(user));

        Flux<User> found = userRepository.findAll();

        StepVerifier.create(found)
                .expectNext(user)
                .verifyComplete();
    }

    @Test
    void findById() {
        when(userRepository.findById(user.getId())).thenReturn(Mono.just(user));

        Mono<User> found = userRepository.findById(user.getId());

        StepVerifier.create(found)
                .expectNext(user)
                .verifyComplete();
    }

    @Test
    void save() {
        when(userRepository.save(user)).thenReturn(Mono.just(user));

        Mono<User> saved = userRepository.save(user);

        StepVerifier.create(saved)
                .expectNext(user)
                .verifyComplete();
    }

    @Test
    void deleteById() {
        when(userRepository.deleteById(user.getId())).thenReturn(Mono.empty());

        Mono<Void> deleted = userRepository.deleteById(user.getId());

        StepVerifier.create(deleted)
                .verifyComplete();
    }
}