package com.kaishui.entitlement.web;

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

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private WebTestClient webTestClient;

    private User user;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(userController).build();

        user = new User();
        //user.setId(new ObjectId()); // Remove this line
        user.setUsername("testuser");
        user.setStaffId("12345");
        user.setEmail("test@example.com");
        user.setCreatedDate(new Date());
        user.setLastModifiedDate(new Date());
        user.setAdGroups(List.of("AD_Users"));
    }

    @Test
    void getAllUsers() {
        User userWithId = new User();
        userWithId.setId(new ObjectId());
        userWithId.setUsername("testuser");
        userWithId.setStaffId("12345");
        userWithId.setEmail("test@example.com");
        userWithId.setCreatedDate(new Date());
        userWithId.setLastModifiedDate(new Date());
        userWithId.setAdGroups(List.of("AD_Users"));
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
                    User actualUser = users.get(0);
                    assertThat(actualUser.getUsername()).isEqualTo(user.getUsername());
                    assertThat(actualUser.getStaffId()).isEqualTo(user.getStaffId());
                    assertThat(actualUser.getEmail()).isEqualTo(user.getEmail());
                });

        verify(userService).getAllUsers();
    }

    @Test
    void getUserById() {
        User userWithId = new User();
        userWithId.setId(new ObjectId());
        userWithId.setUsername("testuser");
        userWithId.setStaffId("12345");
        userWithId.setEmail("test@example.com");
        userWithId.setCreatedDate(new Date());
        userWithId.setLastModifiedDate(new Date());
        userWithId.setAdGroups(List.of("AD_Users"));
        when(userService.getUserById(any(ObjectId.class))).thenReturn(Mono.just(userWithId));

        webTestClient.get().uri("/v1/api/users/{id}", new ObjectId().toHexString())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(User.class)
                .consumeWith(response -> {
                    User actualUser = response.getResponseBody();
                    assertThat(actualUser.getUsername()).isEqualTo(user.getUsername());
                    assertThat(actualUser.getStaffId()).isEqualTo(user.getStaffId());
                    assertThat(actualUser.getEmail()).isEqualTo(user.getEmail());
                });

        verify(userService).getUserById(any(ObjectId.class));
    }

    @Test
    void getUserById_notFound() {
        when(userService.getUserById(any(ObjectId.class))).thenReturn(Mono.empty());

        webTestClient.get().uri("/v1/api/users/{id}", new ObjectId().toHexString())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();

        verify(userService).getUserById(any(ObjectId.class));
    }

    @Test
    void createUser() {
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        User userWithId = new User();
        userWithId.setId(new ObjectId());
        userWithId.setUsername("testuser");
        userWithId.setStaffId("12345");
        userWithId.setEmail("test@example.com");
        userWithId.setCreatedDate(new Date());
        userWithId.setLastModifiedDate(new Date());
        userWithId.setAdGroups(List.of("AD_Users"));
        when(userService.createUser(any(User.class))).thenReturn(Mono.just(userWithId));

        webTestClient.post().uri("/v1/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(User.class)
                .consumeWith(response -> {
                    User actualUser = response.getResponseBody();
                    assertThat(actualUser.getUsername()).isEqualTo(user.getUsername());
                    assertThat(actualUser.getStaffId()).isEqualTo(user.getStaffId());
                    assertThat(actualUser.getEmail()).isEqualTo(user.getEmail());
                });

        verify(userService).createUser(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getUsername()).isEqualTo(user.getUsername());
        assertThat(capturedUser.getStaffId()).isEqualTo(user.getStaffId());
        assertThat(capturedUser.getEmail()).isEqualTo(user.getEmail());
    }

    @Test
    void createUser_duplicateStaffId() {
        when(userService.createUser(any(User.class))).thenReturn(Mono.error(new DuplicateKeyException("Duplicate StaffId")));

        webTestClient.post().uri("/v1/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user)
                .exchange()
                .expectStatus().isEqualTo(409);

        verify(userService).createUser(any(User.class));
    }

    @Test
    void createUser_blankUsername() {
        User userWithBlankUsername = new User();
        userWithBlankUsername.setUsername("");
        userWithBlankUsername.setStaffId("12345");

        webTestClient.post().uri("/v1/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userWithBlankUsername)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void updateUser() {
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        User userWithId = new User();
        userWithId.setId(new ObjectId());
        userWithId.setUsername("testuser");
        userWithId.setStaffId("12345");
        userWithId.setEmail("test@example.com");
        userWithId.setCreatedDate(new Date());
        userWithId.setLastModifiedDate(new Date());
        userWithId.setAdGroups(List.of("AD_Users"));
        when(userService.updateUser(any(User.class))).thenReturn(Mono.just(userWithId));

        webTestClient.put().uri("/v1/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(User.class)
                .consumeWith(response -> {
                    User actualUser = response.getResponseBody();
                    assertThat(actualUser.getUsername()).isEqualTo(user.getUsername());
                    assertThat(actualUser.getStaffId()).isEqualTo(user.getStaffId());
                    assertThat(actualUser.getEmail()).isEqualTo(user.getEmail());
                });

        verify(userService).updateUser(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getUsername()).isEqualTo(user.getUsername());
        assertThat(capturedUser.getStaffId()).isEqualTo(user.getStaffId());
        assertThat(capturedUser.getEmail()).isEqualTo(user.getEmail());
    }

    @Test
    void updateUser_notFound() {
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
        when(userService.updateUser(any(User.class))).thenReturn(Mono.error(new DuplicateKeyException("Duplicate StaffId")));

        webTestClient.put().uri("/v1/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user)
                .exchange()
                .expectStatus().isEqualTo(409);

        verify(userService).updateUser(any(User.class));
    }

    @Test
    void updateUser_blankUsername() {
        User userWithBlankUsername = new User();
        userWithBlankUsername.setId(new ObjectId());
        userWithBlankUsername.setUsername("");
        userWithBlankUsername.setStaffId("12345");

        webTestClient.put().uri("/v1/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userWithBlankUsername)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void deleteUser() {
        ObjectId userId = new ObjectId();
        when(userService.deleteUser(userId)).thenReturn(Mono.empty());

        webTestClient.delete().uri("/v1/api/users/{id}", userId.toHexString())
                .exchange()
                .expectStatus().isNoContent();

        verify(userService).deleteUser(userId);
    }

    @Test
    void deleteUser_notFound() {
        when(userService.deleteUser(any(ObjectId.class))).thenReturn(Mono.empty());

        webTestClient.delete().uri("/v1/api/users/{id}", new ObjectId().toHexString())
                .exchange()
                .expectStatus().isNotFound();

        verify(userService).deleteUser(any(ObjectId.class));
    }
}