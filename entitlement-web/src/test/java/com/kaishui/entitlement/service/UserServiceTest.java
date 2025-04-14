package com.kaishui.entitlement.service;

import com.kaishui.entitlement.entity.GroupDefaultRole;
import com.kaishui.entitlement.entity.User;
import com.kaishui.entitlement.exception.CommonException;
import com.kaishui.entitlement.repository.GroupDefaultRoleRepository;
import com.kaishui.entitlement.repository.UserRepository;
import com.kaishui.entitlement.util.AuthorizationUtil;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupDefaultRoleRepository groupDefaultRoleRepository;

    @InjectMocks
    private UserService userService;
    @Mock
    private AuthorizationUtil authorizationUtil;


    private MockedStatic<AuthorizationUtil> mockedAuthorizationUtil;

    private User user1;
    private User user2;
    private User inactiveUser;
    private User userToCreate;
    private User userToUpdate;
    private User firstLoginUser;
    private GroupDefaultRole groupRole1;
    private GroupDefaultRole groupRole2;

    private final String userId1 = new ObjectId().toHexString();
    private final String userId2 = new ObjectId().toHexString();
    private final String inactiveUserId = new ObjectId().toHexString();
    private final String firstLoginUserId = new ObjectId().toHexString();
    private final String staffId1 = "staff123";
    private final String staffId2 = "staff456";
    private final String inactiveStaffId = "staff789";
    private final String firstLoginStaffId = "staff101";
    private final String testUsername = "testUser";
    private final String adGroup1 = "group-a";
    private final String adGroup2 = "group-b";
    private final String roleId1 = "ROLE_ADMIN";
    private final String roleId2 = "ROLE_USER";
    private final String roleId3 = "ROLE_GUEST";


    @BeforeEach
    void setUp() {
        user1 = User.builder().id(userId1).username("userone").staffId(staffId1).email("one@test.com").isActive(true).isFirstLogin(false).adGroups(List.of(adGroup1)).build();
        user2 = User.builder().id(userId2).username("usertwo").staffId(staffId2).email("two@test.com").isActive(true).isFirstLogin(false).build();
        inactiveUser = User.builder().id(inactiveUserId).username("inactive").staffId(inactiveStaffId).email("inactive@test.com").isActive(false).isFirstLogin(false).build();
        userToCreate = User.builder().username("newuser").staffId("newstaff").email("new@test.com").build(); // No ID initially
        userToUpdate = User.builder().username("updateduser").staffId(staffId1).email("updated@test.com").department("IT").build(); // Use existing staffId1
        firstLoginUser = User.builder().id(firstLoginUserId).username("firstlogin").staffId(firstLoginStaffId).email("first@test.com").isActive(true).isFirstLogin(true).adGroups(List.of(adGroup1, adGroup2)).build();

        groupRole1 = GroupDefaultRole.builder().groupName(adGroup1).roleIds(List.of(roleId1, roleId2)).build();
        groupRole2 = GroupDefaultRole.builder().groupName(adGroup2).roleIds(List.of(roleId2, roleId3)).build();


        // Mock the static method authorizationUtil.extractUsernameFromContext
        mockedAuthorizationUtil = Mockito.mockStatic(AuthorizationUtil.class);
        mockedAuthorizationUtil.when(() -> authorizationUtil.extractUsernameFromContext(any(Context.class)))
                .thenReturn(testUsername);
        // Also mock the ContextView variant if used internally by your actual util or tests
        mockedAuthorizationUtil.when(() -> authorizationUtil.extractUsernameFromContext(any(reactor.util.context.ContextView.class)))
                .thenReturn(testUsername);
    }

    @AfterEach
    void tearDown() {
        // Close the static mock
        mockedAuthorizationUtil.close();
    }

    @Test
    @DisplayName("getAllUsers should return all users from repository")
    void getAllUsers_Success() {
        when(userRepository.findAll()).thenReturn(Flux.just(user1, user2, inactiveUser));

        StepVerifier.create(userService.getAllUsers())
                .expectNext(user1)
                .expectNext(user2)
                .expectNext(inactiveUser)
                .verifyComplete();

        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("getUserById should return user when found")
    void getUserById_Found() {
        when(userRepository.findById(userId1)).thenReturn(Mono.just(user1));

        StepVerifier.create(userService.getUserById(userId1))
                .expectNext(user1)
                .verifyComplete();

        verify(userRepository).findById(userId1);
    }

    @Test
    @DisplayName("getUserById should return empty when not found")
    void getUserById_NotFound() {
        when(userRepository.findById("nonexistent")).thenReturn(Mono.empty());

        StepVerifier.create(userService.getUserById("nonexistent"))
                .expectNextCount(0)
                .verifyComplete();

        verify(userRepository).findById("nonexistent");
    }

    @Test
    @DisplayName("createUser should save the user")
    void createUser_Success() {
        // Arrange
        User inputUser = User.builder().username("createTest").staffId("createStaff").build();
        User savedUser = User.builder().id(new ObjectId().toHexString()).username("createTest").staffId("createStaff").build(); // Simulate saved user with ID
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(savedUser));

        // Act
        Mono<User> result = userService.createUser(inputUser);

        // Assert
        StepVerifier.create(result)
                .expectNext(savedUser)
                .verifyComplete();

        verify(userRepository).save(inputUser); // Verify save was called with the input user
    }

    @Test
    @DisplayName("updateUser should succeed for active user")
    void updateUser_SuccessActiveUser() {
        // Arrange
        User updateData = User.builder().staffId(staffId1).username("updatedName").email("updated@email.com").build();
        // Create a mutable copy for the mock to return and modify
        User existingUserCopy = User.builder()
                .id(user1.getId())
                .username(user1.getUsername())
                .staffId(user1.getStaffId())
                .email(user1.getEmail())
                .isActive(true) // Ensure active
                .createdBy("creator")
                .createdDate(new Date())
                .build();

        when(userRepository.findByStaffId(staffId1)).thenReturn(Mono.just(existingUserCopy));
        // Mock save to return the user passed to it, simulating the save operation
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // Act
        Mono<User> result = userService.updateUser(updateData)
                .contextWrite(ctx -> ctx.put("USER_INFO", testUsername)); // Provide context

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(updatedUser ->
                        updatedUser.getId().equals(userId1) &&
                                updatedUser.getUsername().equals("updatedName") && // Check updated field
                                updatedUser.getEmail().equals("updated@email.com") && // Check updated field
                                updatedUser.getUpdatedBy().equals(testUsername) && // Check audit field
                                updatedUser.getLastModifiedDate() != null // Check audit field
                )
                .verifyComplete();

        verify(userRepository).findByStaffId(staffId1);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("updateUser should fail if staffId is null")
    void updateUser_FailNullStaffId() {
        User updateData = User.builder().staffId(null).username("name").build();

        StepVerifier.create(userService.updateUser(updateData))
                .expectErrorMatches(throwable -> throwable instanceof CommonException &&
                        throwable.getMessage().contains("StaffId is required"))
                .verify();

        verify(userRepository, never()).findByStaffId(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("updateUser should fail if staffId is blank")
    void updateUser_FailBlankStaffId() {
        User updateData = User.builder().staffId(" ").username("name").build();

        StepVerifier.create(userService.updateUser(updateData))
                .expectErrorMatches(throwable -> throwable instanceof CommonException &&
                        throwable.getMessage().contains("StaffId is required"))
                .verify();

        verify(userRepository, never()).findByStaffId(anyString());
        verify(userRepository, never()).save(any(User.class));
    }


    @Test
    @DisplayName("updateUser should fail if user not found")
    void updateUser_FailNotFound() {
        User updateData = User.builder().staffId("nonexistentStaffId").username("name").build();
        when(userRepository.findByStaffId("nonexistentStaffId")).thenReturn(Mono.empty());

        Mono<User> result = userService.updateUser(updateData)
                .contextWrite(ctx -> ctx.put("USER_INFO", testUsername));

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof CommonException &&
                        throwable.getMessage().contains("User not found for update with staffId: nonexistentStaffId"))
                .verify();

        verify(userRepository).findByStaffId("nonexistentStaffId");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("updateUser should fail if user is inactive")
    void updateUser_FailInactiveUser() {
        User updateData = User.builder().staffId(inactiveStaffId).username("name").build();
        when(userRepository.findByStaffId(inactiveStaffId)).thenReturn(Mono.just(inactiveUser)); // Return the inactive user

        Mono<User> result = userService.updateUser(updateData)
                .contextWrite(ctx -> ctx.put("USER_INFO", testUsername));

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof CommonException &&
                        throwable.getMessage().contains("Cannot update inactive user with staffId: " + inactiveStaffId))
                .verify();

        verify(userRepository).findByStaffId(inactiveStaffId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("deleteUser should soft delete active user")
    void deleteUser_SuccessActive() {
        // Arrange
        // Create a mutable copy for the mock to return and modify
        User existingUserCopy = User.builder()
                .id(user1.getId())
                .username(user1.getUsername())
                .staffId(user1.getStaffId())
                .isActive(true) // Ensure active
                .build();
        when(userRepository.findById(userId1)).thenReturn(Mono.just(existingUserCopy));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // Act
        Mono<Void> result = userService.deleteUser(userId1)
                .contextWrite(ctx -> ctx.put("USER_INFO", testUsername));

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(userRepository).findById(userId1);
        verify(userRepository).save(argThat(user ->
                !user.isActive() && // Check if inactive
                        user.getUpdatedBy().equals(testUsername) &&
                        user.getLastModifiedDate() != null
        ));
    }

    @Test
    @DisplayName("deleteUser should complete without saving for already inactive user")
    void deleteUser_AlreadyInactive() {
        // Arrange
        when(userRepository.findById(inactiveUserId)).thenReturn(Mono.just(inactiveUser)); // Return the inactive user

        // Act
        Mono<Void> result = userService.deleteUser(inactiveUserId)
                .contextWrite(ctx -> ctx.put("USER_INFO", testUsername));

        // Assert
        StepVerifier.create(result)
                .verifyComplete(); // Should complete without error

        verify(userRepository).findById(inactiveUserId);
        verify(userRepository, never()).save(any(User.class)); // Save should not be called
    }

    @Test
    @DisplayName("deleteUser should fail when user not found")
    void deleteUser_NotFound() {
        // Arrange
        when(userRepository.findById("nonexistent")).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = userService.deleteUser("nonexistent")
                .contextWrite(ctx -> ctx.put("USER_INFO", testUsername));

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof CommonException &&
                        throwable.getMessage().contains("User not found for deletion with id: nonexistent"))
                .verify();

        verify(userRepository).findById("nonexistent");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("processFirstLogin should do nothing if not first login")
    void processFirstLogin_NotFirstLogin() {
        User notFirstLoginUser = User.builder().isFirstLogin(false).build();

        StepVerifier.create(userService.processFirstLogin(notFirstLoginUser))
                .expectNext(notFirstLoginUser) // Should return the user unmodified
                .verifyComplete();

        verify(groupDefaultRoleRepository, never()).findByGroupNameIn(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("processFirstLogin should assign roles and update flag if first login with groups and roles found")
    void processFirstLogin_Success() {
        // Arrange
        User firstLoginUserCopy = User.builder() // Use a copy
                .id(firstLoginUserId)
                .username("firstlogin")
                .staffId(firstLoginStaffId)
                .isActive(true)
                .isFirstLogin(true)
                .adGroups(List.of(adGroup1, adGroup2))
                .build();

        when(groupDefaultRoleRepository.findByGroupNameIn(List.of(adGroup1, adGroup2)))
                .thenReturn(Flux.just(groupRole1, groupRole2));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // Act
        Mono<User> result = userService.processFirstLogin(firstLoginUserCopy);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(updatedUser ->
                        !updatedUser.isFirstLogin() && // Flag should be false
                                updatedUser.getRoleIds() != null &&
                                updatedUser.getRoleIds().containsAll(List.of(roleId1, roleId2, roleId3)) && // Check assigned roles (distinct)
                                updatedUser.getRoleIds().size() == 3 // Ensure distinct roles
                )
                .verifyComplete();

        verify(groupDefaultRoleRepository).findByGroupNameIn(List.of(adGroup1, adGroup2));
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("processFirstLogin should update flag even if no roles found for groups")
    void processFirstLogin_NoRolesFound() {
        // Arrange
        User firstLoginUserCopy = User.builder() // Use a copy
                .id(firstLoginUserId)
                .username("firstlogin")
                .staffId(firstLoginStaffId)
                .isActive(true)
                .isFirstLogin(true)
                .adGroups(List.of("group-c")) // Group with no roles defined
                .build();
        when(groupDefaultRoleRepository.findByGroupNameIn(List.of("group-c"))).thenReturn(Flux.empty()); // No roles found
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // Act
        Mono<User> result = userService.processFirstLogin(firstLoginUserCopy);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(updatedUser ->
                        !updatedUser.isFirstLogin() && // Flag should be false
                                (updatedUser.getRoleIds() == null || updatedUser.getRoleIds().isEmpty()) // Roles should be empty or null
                )
                .verifyComplete();

        verify(groupDefaultRoleRepository).findByGroupNameIn(List.of("group-c"));
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("processFirstLogin should handle null AD groups")
    void processFirstLogin_NullAdGroups() {
        // Arrange
        User firstLoginUserCopy = User.builder() // Use a copy
                .id(firstLoginUserId)
                .username("firstlogin")
                .staffId(firstLoginStaffId)
                .isActive(true)
                .isFirstLogin(true)
                .adGroups(null) // Null AD groups
                .build();
        // No need to mock groupDefaultRoleRepository as it shouldn't be called
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // Act
        Mono<User> result = userService.processFirstLogin(firstLoginUserCopy);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(updatedUser ->
                        !updatedUser.isFirstLogin() && // Flag should be false
                                (updatedUser.getRoleIds() == null || updatedUser.getRoleIds().isEmpty()) // Roles should be empty or null
                )
                .verifyComplete();

        verify(groupDefaultRoleRepository, never()).findByGroupNameIn(any()); // Should not be called
        verify(userRepository).save(any(User.class)); // Should still save to update the flag
    }

    @Test
    @DisplayName("processFirstLogin should handle empty AD groups")
    void processFirstLogin_EmptyAdGroups() {
        // Arrange
        User firstLoginUserCopy = User.builder() // Use a copy
                .id(firstLoginUserId)
                .username("firstlogin")
                .staffId(firstLoginStaffId)
                .isActive(true)
                .isFirstLogin(true)
                .adGroups(Collections.emptyList()) // Empty AD groups
                .build();
        // No need to mock groupDefaultRoleRepository as it shouldn't be called
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // Act
        Mono<User> result = userService.processFirstLogin(firstLoginUserCopy);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(updatedUser ->
                        !updatedUser.isFirstLogin() && // Flag should be false
                                (updatedUser.getRoleIds() == null || updatedUser.getRoleIds().isEmpty()) // Roles should be empty or null
                )
                .verifyComplete();

        verify(groupDefaultRoleRepository, never()).findByGroupNameIn(any()); // Should not be called
        verify(userRepository).save(any(User.class)); // Should still save to update the flag
    }


    @Test
    @DisplayName("insertOrUpdateUser should insert new user when not found")
    void insertOrUpdateUser_InsertNew() {
        // Arrange
        User newUserInput = User.builder().staffId("newStaff123").username("newUser").email("new@test.com").build();
        User savedUser = User.builder().id(new ObjectId().toHexString()).staffId("newStaff123").username("newUser").email("new@test.com").isActive(true).isFirstLogin(true).createdBy(testUsername).createdDate(new Date()).build(); // Simulate saved user

        when(userRepository.findByStaffId("newStaff123")).thenReturn(Mono.empty());
        // Mock the save for the insert case
        when(userRepository.save(argThat(user -> user.getId() == null && user.getStaffId().equals("newStaff123"))))
                .thenReturn(Mono.just(savedUser));
        // Mock processFirstLogin to return the user passed to it (simplified for this test focus)
        // We assume processFirstLogin works correctly based on its own tests
        when(groupDefaultRoleRepository.findByGroupNameIn(any())).thenReturn(Flux.empty()); // Assume no roles for simplicity
        when(userRepository.save(argThat(user -> user.getId() != null && !user.isFirstLogin()))) // Mock save after processFirstLogin
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));


        // Act
        Mono<User> result = userService.insertOrUpdateUser(newUserInput)
                .contextWrite(ctx -> ctx.put("USER_INFO", testUsername));

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(u ->
                        u.getId() != null &&
                                u.getStaffId().equals("newStaff123") &&
                                u.getCreatedBy().equals(testUsername) && // Check audit field
                                u.isActive() &&
                                !u.isFirstLogin() // Check flag after processFirstLogin
                )
                .verifyComplete();

        verify(userRepository).findByStaffId("newStaff123");
        // Verify save was called twice: once for insert, once after processFirstLogin
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    @DisplayName("insertOrUpdateUser should update existing active user")
    void insertOrUpdateUser_UpdateExistingActive() {
        // Arrange
        User updateInput = User.builder().staffId(staffId1).username("updatedName").email("updated@test.com").department("Sales").build();
        // Create a mutable copy for the mock to return and modify
        User existingUserCopy = User.builder()
                .id(user1.getId())
                .username(user1.getUsername())
                .staffId(user1.getStaffId())
                .email(user1.getEmail())
                .isActive(true) // Ensure active
                .createdBy("creator")
                .createdDate(new Date())
                .isFirstLogin(false) // Assume already logged in
                .build();

        when(userRepository.findByStaffId(staffId1)).thenReturn(Mono.just(existingUserCopy));
        // Mock save for the update case
        when(userRepository.save(argThat(user -> user.getId().equals(userId1))))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // Act
        Mono<User> result = userService.insertOrUpdateUser(updateInput)
                .contextWrite(ctx -> ctx.put("USER_INFO", testUsername));

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(u ->
                        u.getId().equals(userId1) &&
                                u.getUsername().equals("updatedName") && // Check updated field
                                u.getEmail().equals("updated@test.com") && // Check updated field
                                u.getDepartment().equals("Sales") && // Check updated field
                                u.getUpdatedBy().equals(testUsername) && // Check audit field
                                u.getLastModifiedDate() != null // Check audit field
                )
                .verifyComplete();

        verify(userRepository).findByStaffId(staffId1);
        verify(userRepository).save(any(User.class)); // Verify save was called once for update
        verify(groupDefaultRoleRepository, never()).findByGroupNameIn(any()); // processFirstLogin should not run
    }

    @Test
    @DisplayName("insertOrUpdateUser should fail for existing inactive user")
    void insertOrUpdateUser_FailUpdateInactive() {
        // Arrange
        User updateInput = User.builder().staffId(inactiveStaffId).username("updatedName").build();
        when(userRepository.findByStaffId(inactiveStaffId)).thenReturn(Mono.just(inactiveUser)); // Return inactive user

        // Act
        Mono<User> result = userService.insertOrUpdateUser(updateInput)
                .contextWrite(ctx -> ctx.put("USER_INFO", testUsername));

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof CommonException &&
                        throwable.getMessage().contains("Cannot update inactive user with staffId: " + inactiveStaffId))
                .verify();

        verify(userRepository).findByStaffId(inactiveStaffId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("insertOrUpdateUser should fail if staffId is null")
    void insertOrUpdateUser_FailNullStaffId() {
        User input = User.builder().staffId(null).username("name").build();

        StepVerifier.create(userService.insertOrUpdateUser(input))
                .expectErrorMatches(throwable -> throwable instanceof CommonException &&
                        throwable.getMessage().contains("StaffId is required"))
                .verify();

        verify(userRepository, never()).findByStaffId(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("insertOrUpdateUser should fail if staffId is blank")
    void insertOrUpdateUser_FailBlankStaffId() {
        User input = User.builder().staffId(" ").username("name").build();

        StepVerifier.create(userService.insertOrUpdateUser(input))
                .expectErrorMatches(throwable -> throwable instanceof CommonException &&
                        throwable.getMessage().contains("StaffId is required"))
                .verify();

        verify(userRepository, never()).findByStaffId(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}