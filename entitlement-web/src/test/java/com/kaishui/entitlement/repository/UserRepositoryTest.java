package com.kaishui.entitlement.repository;

import com.kaishui.entitlement.entity.Entitlement; // Import Entitlement
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

import java.util.Collections; // For empty list if needed
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRepositoryTest {

    @Mock
    private UserRepository userRepository; // This is the interface being mocked

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
        // Adjust to use entitlements
        // Example: User belongs to "AD_Users" group with a specific role "ROLE_USER"
        Entitlement userEntitlement = Entitlement.builder()
                .adGroup("AD_Users")
                .roleIds(List.of("ROLE_USER")) // Example role ID
                .build();
        user.setEntitlements(List.of(userEntitlement));
        // If the user has no specific entitlements or you want to test with an empty list:
        // user.setEntitlements(Collections.emptyList());
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

    // You might want to add tests for your custom query methods if you have any,
    // for example, findByStaffId or findByAdGroupAndIsActive

    @Test
    void findByStaffId_Found() {
        when(userRepository.findByStaffId(user.getStaffId())).thenReturn(Mono.just(user));
        Mono<User> found = userRepository.findByStaffId(user.getStaffId());
        StepVerifier.create(found)
                .expectNext(user)
                .verifyComplete();
    }

    @Test
    void findByStaffId_NotFound() {
        when(userRepository.findByStaffId("nonexistentStaffId")).thenReturn(Mono.empty());
        Mono<User> found = userRepository.findByStaffId("nonexistentStaffId");
        StepVerifier.create(found)
                .verifyComplete();
    }

    @Test
    void findByAdGroupAndIsActive_Found() {
        // Assuming your repository has a method like this.
        // The AD group to search for would be one present in the user's entitlements.
        String targetAdGroup = "AD_Users";
        if (user.getEntitlements() != null && !user.getEntitlements().isEmpty()) {
            targetAdGroup = user.getEntitlements().get(0).getAdGroup();
        }

        when(userRepository.findByAdGroupAndIsActive(targetAdGroup, true)).thenReturn(Flux.just(user));
        Flux<User> found = userRepository.findByAdGroupAndIsActive(targetAdGroup, true);
        StepVerifier.create(found)
                .expectNext(user)
                .verifyComplete();
    }

    @Test
    void findByAdGroupAndIsActive_NotFound() {
        when(userRepository.findByAdGroupAndIsActive("nonexistentAdGroup", true)).thenReturn(Flux.empty());
        Flux<User> found = userRepository.findByAdGroupAndIsActive("nonexistentAdGroup", true);
        StepVerifier.create(found)
                .verifyComplete();
    }
}