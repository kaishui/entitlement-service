package com.kaishui.entitlement.web;

import com.kaishui.entitlement.entity.Entitlement; // Import Entitlement
import com.kaishui.entitlement.entity.User;
import com.kaishui.entitlement.service.UserService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections; // For empty list if needed
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private WebTestClient webTestClient;

    private User user; // User object for request bodies
    private User userWithId; // User object for service responses

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(userController).build();

        // User object for request bodies (typically without ID)
        user = new User();
        user.setUsername("testuser");
        user.setStaffId("12345");
        user.setEmail("test@example.com");
        user.setCreatedDate(new Date()); // Usually not set by client, but for consistency
        user.setLastModifiedDate(new Date()); // Usually not set by client
        // Adjust to use entitlements
        Entitlement requestEntitlement = Entitlement.builder()
                .adGroup("AD_Users_Request") // Example AD group
                .roleIds(List.of("ROLE_USER_FROM_REQUEST")) // Example role ID
                .build();
        user.setEntitlements(List.of(requestEntitlement));

        // User object for service responses (typically with ID)
        userWithId = new User();
        userWithId.setId(new ObjectId().toHexString()); // ID is set for responses
        userWithId.setUsername("testuser");
        userWithId.setStaffId("12345");
        userWithId.setEmail("test@example.com");
        userWithId.setCreatedDate(new Date());
        userWithId.setLastModifiedDate(new Date());
        Entitlement responseEntitlement = Entitlement.builder()
                .adGroup("AD_Users_Response")
                .roleIds(List.of("ROLE_USER_FROM_RESPONSE"))
                .build();
        userWithId.setEntitlements(List.of(responseEntitlement));
    }

    @Test
    void getAllUsers() {
        // userWithId is already set up with an ID and entitlements in setUp()
        when(userService.getAllUsers()).thenReturn(Flux.just(userWithId));

        webTestClient.get().uri("/v1/api/users")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(User.class)
                .hasSize(1)
                .consumeWith(response -> {
                    List<User> users = response.getResponseBody();
                    assertThat(users).isNotNull();
                    User actualUser = users.get(0);
                    assertThat(actualUser.getUsername()).isEqualTo(userWithId.getUsername());
                    assertThat(actualUser.getStaffId()).isEqualTo(userWithId.getStaffId());
                    assertThat(actualUser.getEmail()).isEqualTo(userWithId.getEmail());
                    // Optionally, assert entitlements if they are expected to be the same
                    assertThat(actualUser.getEntitlements()).isEqualTo(userWithId.getEntitlements());
                });

        verify(userService).getAllUsers();
    }

    @Test
    void getUserById() {
        // userWithId is set up
        String testId = userWithId.getId();
        when(userService.getUserById(testId)).thenReturn(Mono.just(userWithId));

        webTestClient.get().uri("/v1/api/users/{id}", testId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(User.class)
                .consumeWith(response -> {
                    User actualUser = response.getResponseBody();
                    assertThat(actualUser).isNotNull();
                    assertThat(actualUser.getUsername()).isEqualTo(userWithId.getUsername());
                    assertThat(actualUser.getStaffId()).isEqualTo(userWithId.getStaffId());
                    assertThat(actualUser.getEmail()).isEqualTo(userWithId.getEmail());
                    assertThat(actualUser.getEntitlements()).isEqualTo(userWithId.getEntitlements());
                });

        verify(userService).getUserById(testId);
    }

    @Test
    void getUserById_notFound() {
        String testId = new ObjectId().toHexString();
        when(userService.getUserById(testId)).thenReturn(Mono.empty());

        webTestClient.get().uri("/v1/api/users/{id}", testId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();

        verify(userService).getUserById(testId);
    }

    @Test
    void createUser() {
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        // 'user' is the request body (without ID), 'userWithId' is the expected response
        when(userService.createUser(any(User.class))).thenReturn(Mono.just(userWithId));

        webTestClient.post().uri("/v1/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user) // Send the 'user' object (without ID)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(User.class)
                .consumeWith(response -> {
                    User actualUser = response.getResponseBody();
                    assertThat(actualUser).isNotNull();
                    assertThat(actualUser.getId()).isEqualTo(userWithId.getId()); // Check ID from response
                    assertThat(actualUser.getUsername()).isEqualTo(user.getUsername()); // Compare with request data
                    assertThat(actualUser.getStaffId()).isEqualTo(user.getStaffId());
                    assertThat(actualUser.getEmail()).isEqualTo(user.getEmail());
                    // Assert that the response entitlements match what the service returned
                    assertThat(actualUser.getEntitlements()).isEqualTo(userWithId.getEntitlements());
                });

        verify(userService).createUser(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getUsername()).isEqualTo(user.getUsername());
        assertThat(capturedUser.getStaffId()).isEqualTo(user.getStaffId());
        assertThat(capturedUser.getEmail()).isEqualTo(user.getEmail());
        assertThat(capturedUser.getEntitlements()).isEqualTo(user.getEntitlements()); // Verify captured request entitlements
    }

    @Test
    void createUser_duplicateStaffId() {
        // 'user' is the request body
        when(userService.createUser(any(User.class))).thenReturn(Mono.error(new DuplicateKeyException("Duplicate StaffId")));

        webTestClient.post().uri("/v1/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user)
                .exchange()
                .expectStatus().isEqualTo(409); // Conflict

        verify(userService).createUser(any(User.class));
    }

    @Test
    void createUser_blankUsername() {
        User userWithBlankUsername = new User();
        userWithBlankUsername.setUsername(""); // Invalid
        userWithBlankUsername.setStaffId("12345");
        // Entitlements can be empty or null for this validation test
        userWithBlankUsername.setEntitlements(Collections.emptyList());


        webTestClient.post().uri("/v1/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userWithBlankUsername)
                .exchange()
                .expectStatus().isBadRequest(); // Assuming @NotBlank on User.username
    }

    @Test
    void updateUser() {
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        // 'user' is the request body, 'userWithId' is the expected response
        when(userService.updateUser(any(User.class))).thenReturn(Mono.just(userWithId));

        webTestClient.put().uri("/v1/api/users") // Assuming PUT updates the whole resource
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user) // Send the 'user' object (which might or might not have an ID for PUT)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(User.class)
                .consumeWith(response -> {
                    User actualUser = response.getResponseBody();
                    assertThat(actualUser).isNotNull();
                    assertThat(actualUser.getId()).isEqualTo(userWithId.getId());
                    assertThat(actualUser.getUsername()).isEqualTo(user.getUsername());
                    assertThat(actualUser.getStaffId()).isEqualTo(user.getStaffId());
                    assertThat(actualUser.getEntitlements()).isEqualTo(userWithId.getEntitlements());
                });

        verify(userService).updateUser(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getUsername()).isEqualTo(user.getUsername());
        assertThat(capturedUser.getStaffId()).isEqualTo(user.getStaffId());
        assertThat(capturedUser.getEntitlements()).isEqualTo(user.getEntitlements());
    }

    @Test
    void updateUser_notFound() {
        // 'user' is the request body
        when(userService.updateUser(any(User.class))).thenReturn(Mono.empty());

        webTestClient.put().uri("/v1/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user)
                .exchange()
                .expectStatus().isNotFound();

        verify(userService).updateUser(any(User.class));
    }

    @Test
    void updateUser_duplicateStaffId() {
        // 'user' is the request body
        when(userService.updateUser(any(User.class))).thenReturn(Mono.error(new DuplicateKeyException("Duplicate StaffId")));

        webTestClient.put().uri("/v1/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user)
                .exchange()
                .expectStatus().isEqualTo(409); // Conflict

        verify(userService).updateUser(any(User.class));
    }

    @Test
    void updateUser_blankUsername() {
        User userWithBlankUsername = new User();
        // For PUT, an ID might be expected in the path or body depending on your API design.
        // Here, assuming it's in the body for simplicity of the User object.
        userWithBlankUsername.setId(new ObjectId().toHexString());
        userWithBlankUsername.setUsername(""); // Invalid
        userWithBlankUsername.setStaffId("12345");
        userWithBlankUsername.setEntitlements(Collections.emptyList());


        webTestClient.put().uri("/v1/api/users") // Or "/v1/api/users/{id}" if ID is path param
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userWithBlankUsername)
                .exchange()
                .expectStatus().isBadRequest(); // Assuming @NotBlank on User.username
    }

    @Test
    void deleteUser() {
        String userIdToDelete = new ObjectId().toHexString();
        when(userService.deleteUser(userIdToDelete)).thenReturn(Mono.empty());

        webTestClient.delete().uri("/v1/api/users/{id}", userIdToDelete)
                .exchange()
                .expectStatus().isNoContent();

        verify(userService).deleteUser(userIdToDelete);
    }
}