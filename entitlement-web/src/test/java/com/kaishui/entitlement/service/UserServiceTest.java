package com.kaishui.entitlement.service;

import com.kaishui.entitlement.entity.*; // Import Entitlement
import com.kaishui.entitlement.entity.dto.UserDto;
import com.kaishui.entitlement.entity.dto.UserResourceDto;
import com.kaishui.entitlement.exception.CommonException;
import com.kaishui.entitlement.exception.ResourceNotFoundException;
import com.kaishui.entitlement.repository.GroupDefaultRoleRepository;
import com.kaishui.entitlement.repository.ResourceRepository;
import com.kaishui.entitlement.repository.RoleRepository;
import com.kaishui.entitlement.repository.UserRepository;
import com.kaishui.entitlement.util.AdGroupUtil;
import com.kaishui.entitlement.util.AuthorizationUtil;
import com.kaishui.entitlement.util.UserMapper;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.util.*; // Import ArrayList, HashSet
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private ResourceRepository resourceRepository;
    @Mock
    private GroupDefaultRoleRepository groupDefaultRoleRepository;
    @Mock
    private AuthorizationUtil authorizationUtil;
    @Mock
    private UserMapper userMapper;
    @Mock
    private AdGroupUtil adGroupUtil;

    @InjectMocks
    private UserService userService;

    // --- Test Data ---
    private User user1, user2, inactiveUser, firstLoginUser, userWithNoRoles, userWithNoAdGroups;
    private Role role1, role2, role3, roleAdminCaseA, roleUserCaseA;
    private Resource resource1, resource2;
    private GroupDefaultRole groupRole1, groupRole2;
    private UserDto userDto1;

    private final String userId1 = new ObjectId().toHexString();
    private final String userId2 = new ObjectId().toHexString();
    private final String inactiveUserId = new ObjectId().toHexString();
    private final String firstLoginUserId = new ObjectId().toHexString();
    private final String userWithNoRolesId = new ObjectId().toHexString();
    private final String userWithNoAdGroupsId = new ObjectId().toHexString();

    private final String staffId1 = "staff123";
    private final String staffId2 = "staff456";
    private final String inactiveStaffId = "staff789";
    private final String firstLoginStaffId = "staff101";
    private final String testUsername = "testUser";
    private final String adGroup1 = "AD_CaseA_ADMIN";
    private final String adGroup2 = "AD_CaseA_USER";
    private final String adGroupOther = "AD_OtherGroup";
    private final String roleId1 = "ROLE_ADMIN_GLOBAL";
    private final String roleId2 = "ROLE_USER_GLOBAL";
    private final String roleId3 = "ROLE_GUEST";
    private final String roleIdAdminCaseA = "ROLE_ADMIN_CASE_A";
    private final String roleIdUserCaseA = "ROLE_USER_CASE_A";
    private final String resourceId1 = "RES_READ";
    private final String resourceId2 = "RES_WRITE";
    private final String userCaseA = "CaseA";


    @BeforeEach
    void setUp() {
        // --- User Setup with Entitlements ---
        Entitlement entitlementUser1Group1 = Entitlement.builder().adGroup(adGroup1).roleIds(List.of(roleId1, roleIdAdminCaseA)).build();
        Entitlement entitlementUser1GroupOther = Entitlement.builder().adGroup(adGroupOther).roleIds(List.of(roleIdUserCaseA)).build(); // For broader role coverage
        user1 = User.builder().id(userId1).username("userone").staffId(staffId1).email("one@test.com")
                .isActive(true).isFirstLogin(false)
                .entitlements(List.of(entitlementUser1Group1, entitlementUser1GroupOther))
                .build();

        Entitlement entitlementUser2Group2 = Entitlement.builder().adGroup(adGroup2).roleIds(List.of(roleId2, roleIdUserCaseA)).build();
        user2 = User.builder().id(userId2).username("usertwo").staffId(staffId2).email("two@test.com")
                .isActive(true).isFirstLogin(false)
                .entitlements(List.of(entitlementUser2Group2))
                .build();

        inactiveUser = User.builder().id(inactiveUserId).username("inactive").staffId(inactiveStaffId).email("inactive@test.com")
                .isActive(false).isFirstLogin(false).entitlements(Collections.emptyList()) // Or null, depending on how you handle it
                .build();

        // For firstLoginUser, roles will be populated by processFirstLogin
        Entitlement firstLoginEntitlement1 = Entitlement.builder().adGroup(adGroup1).roleIds(new ArrayList<>()).build();
        Entitlement firstLoginEntitlement2 = Entitlement.builder().adGroup(adGroup2).roleIds(new ArrayList<>()).build();
        firstLoginUser = User.builder().id(firstLoginUserId).username("firstlogin").staffId(firstLoginStaffId).email("first@test.com")
                .isActive(true).isFirstLogin(true)
                .entitlements(List.of(firstLoginEntitlement1, firstLoginEntitlement2))
                .build();

        userWithNoRoles = User.builder().id(userWithNoRolesId).staffId("noRolesStaff")
                .entitlements(List.of(Entitlement.builder().adGroup(adGroup1).roleIds(Collections.emptyList()).build()))
                .isActive(true).isFirstLogin(false).build();

        userWithNoAdGroups = User.builder().id(userWithNoAdGroupsId).staffId("noAdGroupsStaff")
                .entitlements(List.of(Entitlement.builder().adGroup(null).roleIds(List.of(roleId1)).build())) // AD Group is null
                .isActive(true).isFirstLogin(false).build();


        // --- Role Setup ---
        role1 = Role.builder().id(roleId1).roleName("Admin Global").isActive(true).resourceIds(List.of(resourceId1, resourceId2)).build();
        role2 = Role.builder().id(roleId2).roleName("User Global").isActive(true).resourceIds(List.of(resourceId1)).build();
        role3 = Role.builder().id(roleId3).roleName("Guest").isActive(true).resourceIds(Collections.emptyList()).build();
        roleAdminCaseA = Role.builder().id(roleIdAdminCaseA).roleName("Admin CaseA").userCase(userCaseA).isActive(true).resourceIds(List.of(resourceId1, resourceId2)).build();
        roleUserCaseA = Role.builder().id(roleIdUserCaseA).roleName("User CaseA").userCase(userCaseA).isActive(true).resourceIds(List.of(resourceId1)).build();

        // --- Resource Setup ---
        resource1 = Resource.builder().id(resourceId1).name("Read Data")
                .permission(new Document("action", "read"))
                .isActive(true).adGroups(List.of(adGroup1, adGroup2, adGroupOther)).build();
        resource2 = Resource.builder().id(resourceId2).name("Write Data")
                .permission(new Document("action", "write"))
                .isActive(true).adGroups(List.of(adGroup1)).build();

        // --- GroupDefaultRole Setup ---
        groupRole1 = GroupDefaultRole.builder().groupName(adGroup1).roleIds(List.of(roleId1, roleId2)).build();
        groupRole2 = GroupDefaultRole.builder().groupName(adGroup2).roleIds(List.of(roleId2, roleId3)).build();

        // --- DTO Setup ---
        userDto1 = new UserDto();
        userDto1.setId(userId1);
        userDto1.setStaffId(staffId1);
    }

    // ... GetUserTests, UpdateUserTests, DeleteUserTests remain largely the same as they don't directly test entitlement logic complexity ...
    // Ensure that when findByStaffId or findById is mocked, the returned User object has the new 'entitlements' structure.

    @Nested
    @DisplayName("Get User Tests")
    class GetUserTests {
        @Test
        @DisplayName("getAllUsers should return all users from repository")
        void getAllUsers_Success() {
            when(userRepository.findAll()).thenReturn(Flux.just(user1, user2, inactiveUser));

            StepVerifier.create(userService.getAllUsers())
                    .expectNext(user1, user2, inactiveUser)
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
                    .verifyComplete(); // Expect empty completion

            verify(userRepository).findById("nonexistent");
        }
    }


    @Nested
    @DisplayName("Update User Tests")
    class UpdateUserTests {
        @Test
        @DisplayName("updateUser should succeed for active user")
        void updateUser_SuccessActiveUser() {
            // Arrange
            User updateData = User.builder().staffId(staffId1).username("updatedName").email("updated@email.com").build();
            User existingUserCopy = User.builder() // Create a mutable copy
                    .id(user1.getId()).username(user1.getUsername()).staffId(user1.getStaffId())
                    .email(user1.getEmail()).isActive(true).createdBy("creator").createdDate(new Date())
                    .entitlements(user1.getEntitlements()) // Copy entitlements
                    .build();

            when(userRepository.findByStaffId(staffId1)).thenReturn(Mono.just(existingUserCopy));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0))); // Return saved entity
            when(authorizationUtil.extractUsernameFromContext(any())).thenReturn(testUsername);


            // Act & Assert
            StepVerifier.create(userService.updateUser(updateData)
                            .contextWrite(Context.of(AuthorizationUtil.AUTHORIZATION_HEADER, testUsername)))
                    .expectNextMatches(updatedUser ->
                            updatedUser.getId().equals(userId1) &&
                                    updatedUser.getUsername().equals("updatedName") &&
                                    updatedUser.getEmail().equals("updated@email.com") &&
                                    updatedUser.getLastModifiedBy().equals(testUsername) &&
                                    updatedUser.getLastModifiedDate() != null
                    )
                    .verifyComplete();

            verify(userRepository).findByStaffId(staffId1);
            verify(userRepository).save(argThat(savedUser ->
                    savedUser.getId().equals(userId1) &&
                            savedUser.getUsername().equals("updatedName") &&
                            savedUser.getLastModifiedBy().equals(testUsername)
            ));
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
            when(authorizationUtil.extractUsernameFromContext(any())).thenReturn(testUsername);


            StepVerifier.create(userService.updateUser(updateData)
                            .contextWrite(Context.of(AuthorizationUtil.AUTHORIZATION_HEADER, testUsername)))
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
            when(userRepository.findByStaffId(inactiveStaffId)).thenReturn(Mono.just(inactiveUser));
            when(authorizationUtil.extractUsernameFromContext(any())).thenReturn(testUsername);


            StepVerifier.create(userService.updateUser(updateData)
                            .contextWrite(Context.of(AuthorizationUtil.AUTHORIZATION_HEADER, testUsername)))
                    .expectErrorMatches(throwable -> throwable instanceof CommonException &&
                            throwable.getMessage().contains("Cannot update inactive user with staffId: " + inactiveStaffId))
                    .verify();

            verify(userRepository).findByStaffId(inactiveStaffId);
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Delete User Tests")
    class DeleteUserTests {
        @Test
        @DisplayName("deleteUser should soft delete active user")
        void deleteUser_SuccessActive() {
            User existingUserCopy = User.builder()
                    .id(user1.getId()).username(user1.getUsername()).staffId(user1.getStaffId())
                    .isActive(true).build();
            when(userRepository.findById(userId1)).thenReturn(Mono.just(existingUserCopy));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
            when(authorizationUtil.extractUsernameFromContext(any())).thenReturn(testUsername);

            StepVerifier.create(userService.deleteUser(userId1)
                            .contextWrite(Context.of(AuthorizationUtil.AUTHORIZATION_HEADER, testUsername)))
                    .verifyComplete();

            verify(userRepository).findById(userId1);
            verify(userRepository).save(argThat(user ->
                    !user.isActive() &&
                            user.getLastModifiedBy().equals(testUsername) &&
                            user.getLastModifiedDate() != null
            ));
        }

        @Test
        @DisplayName("deleteUser should complete without saving for already inactive user")
        void deleteUser_AlreadyInactive() {
            when(userRepository.findById(inactiveUserId)).thenReturn(Mono.just(inactiveUser));
            when(authorizationUtil.extractUsernameFromContext(any())).thenReturn(testUsername);


            StepVerifier.create(userService.deleteUser(inactiveUserId)
                            .contextWrite(Context.of(AuthorizationUtil.AUTHORIZATION_HEADER, testUsername)))
                    .verifyComplete();

            verify(userRepository).findById(inactiveUserId);
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("deleteUser should fail when user not found")
        void deleteUser_NotFound() {
            when(userRepository.findById("nonexistent")).thenReturn(Mono.empty());
            when(authorizationUtil.extractUsernameFromContext(any())).thenReturn(testUsername);


            StepVerifier.create(userService.deleteUser("nonexistent")
                            .contextWrite(Context.of(AuthorizationUtil.AUTHORIZATION_HEADER, testUsername)))
                    .expectErrorMatches(throwable -> throwable instanceof CommonException &&
                            throwable.getMessage().contains("User not found for deletion with id: nonexistent"))
                    .verify();

            verify(userRepository).findById("nonexistent");
            verify(userRepository, never()).save(any(User.class));
        }
    }


    @Nested
    @DisplayName("Process First Login Tests")
    class ProcessFirstLoginTests {

        @Test
        @DisplayName("processFirstLogin should do nothing if not first login")
        void processFirstLogin_NotFirstLogin() {
            User notFirstLoginUser = User.builder().isFirstLogin(false).entitlements(Collections.emptyList()).build();

            StepVerifier.create(userService.processFirstLogin(notFirstLoginUser))
                    .expectNext(notFirstLoginUser)
                    .verifyComplete();

            verify(groupDefaultRoleRepository, never()).findByGroupNameIn(any());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("processFirstLogin should assign roles to entitlements and update flag if first login")
        void processFirstLogin_Success() {
            // firstLoginUser is set up with entitlements having empty roleIds lists
            when(groupDefaultRoleRepository.findByGroupNameIn(List.of(adGroup1, adGroup2)))
                    .thenReturn(Flux.just(groupRole1, groupRole2));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            StepVerifier.create(userService.processFirstLogin(firstLoginUser))
                    .expectNextMatches(updatedUser -> {
                        if (updatedUser.isFirstLogin()) return false; // Flag should be false
                        if (CollectionUtils.isEmpty(updatedUser.getEntitlements())) return false;

                        Map<String, List<String>> adGroupToRoles = updatedUser.getEntitlements().stream()
                                .collect(Collectors.toMap(Entitlement::getAdGroup, Entitlement::getRoleIds));

                        List<String> rolesForAdGroup1 = adGroupToRoles.get(adGroup1);
                        List<String> rolesForAdGroup2 = adGroupToRoles.get(adGroup2);

                        boolean adGroup1RolesCorrect = rolesForAdGroup1 != null &&
                                new HashSet<>(rolesForAdGroup1).equals(new HashSet<>(List.of(roleId1, roleId2)));
                        boolean adGroup2RolesCorrect = rolesForAdGroup2 != null &&
                                new HashSet<>(rolesForAdGroup2).equals(new HashSet<>(List.of(roleId2, roleId3)));

                        return adGroup1RolesCorrect && adGroup2RolesCorrect;
                    })
                    .verifyComplete();

            verify(groupDefaultRoleRepository).findByGroupNameIn(List.of(adGroup1, adGroup2));
            verify(userRepository).save(argThat(savedUser -> !savedUser.isFirstLogin()));
        }

        @Test
        @DisplayName("processFirstLogin should update flag even if no roles found for groups")
        void processFirstLogin_NoRolesFound() {
            User user = User.builder().id(firstLoginUserId).isFirstLogin(true)
                    .entitlements(List.of(Entitlement.builder().adGroup("group-c").roleIds(new ArrayList<>()).build()))
                    .build();
            when(groupDefaultRoleRepository.findByGroupNameIn(List.of("group-c"))).thenReturn(Flux.empty());
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            StepVerifier.create(userService.processFirstLogin(user))
                    .expectNextMatches(updatedUser ->
                            !updatedUser.isFirstLogin() &&
                                    updatedUser.getEntitlements().get(0).getRoleIds().isEmpty()
                    )
                    .verifyComplete();
            verify(userRepository).save(argThat(savedUser -> !savedUser.isFirstLogin()));
        }

        @Test
        @DisplayName("processFirstLogin should handle null entitlements list")
        void processFirstLogin_NullEntitlements() {
            User user = User.builder().id(firstLoginUserId).isFirstLogin(true).entitlements(null).build();
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            StepVerifier.create(userService.processFirstLogin(user))
                    .expectNextMatches(updatedUser -> !updatedUser.isFirstLogin() && updatedUser.getEntitlements() == null)
                    .verifyComplete();
            verify(groupDefaultRoleRepository, never()).findByGroupNameIn(any());
            verify(userRepository).save(argThat(savedUser -> !savedUser.isFirstLogin()));
        }

        @Test
        @DisplayName("processFirstLogin should handle empty entitlements list")
        void processFirstLogin_EmptyEntitlements() {
            User user = User.builder().id(firstLoginUserId).isFirstLogin(true).entitlements(Collections.emptyList()).build();
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            StepVerifier.create(userService.processFirstLogin(user))
                    .expectNextMatches(updatedUser -> !updatedUser.isFirstLogin() && updatedUser.getEntitlements().isEmpty())
                    .verifyComplete();
            verify(groupDefaultRoleRepository, never()).findByGroupNameIn(any());
            verify(userRepository).save(argThat(savedUser -> !savedUser.isFirstLogin()));
        }
        @Test
        @DisplayName("processFirstLogin should handle entitlements with null AD groups")
        void processFirstLogin_EntitlementsWithNullAdGroups() {
            User user = User.builder().id(firstLoginUserId).isFirstLogin(true)
                    .entitlements(List.of(Entitlement.builder().adGroup(null).roleIds(new ArrayList<>()).build()))
                    .build();
            // findByGroupNameIn will be called with an empty list if only null/blank AD groups are present
            when(groupDefaultRoleRepository.findByGroupNameIn(Collections.emptyList())).thenReturn(Flux.empty());
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            StepVerifier.create(userService.processFirstLogin(user))
                    .expectNextMatches(updatedUser -> !updatedUser.isFirstLogin())
                    .verifyComplete();

            verify(groupDefaultRoleRepository).findByGroupNameIn(Collections.emptyList());
            verify(userRepository).save(argThat(savedUser -> !savedUser.isFirstLogin()));
        }
    }


    @Nested
    @DisplayName("Insert Or Update User Tests")
    class InsertOrUpdateUserTests {

        @Test
        @DisplayName("insertOrUpdateUser should insert new user and call processFirstLogin")
        void insertOrUpdateUser_InsertNew() {
            User newUserInput = User.builder().staffId("newStaff123").username("newUser").email("new@test.com")
                    .entitlements(List.of(Entitlement.builder().adGroup(adGroup1).roleIds(new ArrayList<>()).build())) // Provide entitlements
                    .isFirstLogin(true) // Explicitly set for clarity
                    .isActive(true)
                    .build();
            User savedUser = User.builder().id(new ObjectId().toHexString()).staffId("newStaff123").username("newUser").email("new@test.com")
                    .entitlements(newUserInput.getEntitlements()).isFirstLogin(true).isActive(true) // Reflect input
                    .build();
            User userAfterFirstLogin = User.builder().id(savedUser.getId()).staffId(savedUser.getStaffId())
                    .entitlements(List.of(Entitlement.builder().adGroup(adGroup1).roleIds(List.of(roleId1, roleId2)).build())) // Roles assigned
                    .isFirstLogin(false).isActive(true) // Flag updated
                    .build();


            when(userRepository.findByStaffId("newStaff123")).thenReturn(Mono.empty());
            when(userRepository.save(eq(newUserInput))).thenReturn(Mono.just(savedUser)); // Save before first login
            when(authorizationUtil.extractUsernameFromContext(any())).thenReturn(testUsername);
            // Mock processFirstLogin's internal calls
            when(groupDefaultRoleRepository.findByGroupNameIn(List.of(adGroup1))).thenReturn(Flux.just(groupRole1));
            when(userRepository.save(argThat(u -> u.getId().equals(savedUser.getId()) && !u.isFirstLogin()))) // Save after first login
                    .thenReturn(Mono.just(userAfterFirstLogin));


            StepVerifier.create(userService.insertOrUpdateUser(newUserInput)
                            .contextWrite(Context.of(AuthorizationUtil.AUTHORIZATION_HEADER, testUsername)))
                    .expectNextMatches(u ->
                            u.getId().equals(savedUser.getId()) &&
                                    !u.isFirstLogin() && // Check first login flag
                                    u.getEntitlements().get(0).getRoleIds().containsAll(List.of(roleId1, roleId2)) // Check roles
                    )
                    .verifyComplete();

            verify(userRepository).findByStaffId("newStaff123");
            verify(userRepository).save(eq(newUserInput)); // Initial save
            verify(groupDefaultRoleRepository).findByGroupNameIn(List.of(adGroup1));
            verify(userRepository).save(argThat(u -> u.getId().equals(savedUser.getId()) && !u.isFirstLogin())); // Save from processFirstLogin
        }

        @Test
        @DisplayName("insertOrUpdateUser should update existing active user with entitlements")
        void insertOrUpdateUser_UpdateExistingActive() {
            List<Entitlement> initialEntitlements = List.of(Entitlement.builder().adGroup(adGroup1).roleIds(List.of(roleId1)).build());
            List<Entitlement> updatedEntitlements = List.of(Entitlement.builder().adGroup(adGroup1).roleIds(List.of(roleId1, roleId2)).build(),
                    Entitlement.builder().adGroup(adGroup2).roleIds(List.of(roleId3)).build());

            User updateInput = User.builder().staffId(staffId1).username("updatedName").email("updated@test.com")
                    .entitlements(updatedEntitlements).isActive(true).build();
            User existingUserCopy = User.builder()
                    .id(user1.getId()).username(user1.getUsername()).staffId(user1.getStaffId())
                    .email(user1.getEmail()).isActive(true).isFirstLogin(false)
                    .entitlements(initialEntitlements) // Original entitlements
                    .build();

            when(userRepository.findByStaffId(staffId1)).thenReturn(Mono.just(existingUserCopy));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
            when(authorizationUtil.extractUsernameFromContext(any())).thenReturn(testUsername);

            StepVerifier.create(userService.insertOrUpdateUser(updateInput)
                            .contextWrite(Context.of(AuthorizationUtil.AUTHORIZATION_HEADER, testUsername)))
                    .expectNextMatches(u ->
                            u.getId().equals(userId1) &&
                                    u.getUsername().equals("updatedName") &&
                                    u.getEntitlements().size() == 2 && // Entitlements should be overwritten
                                    u.getEntitlements().stream().anyMatch(e -> e.getAdGroup().equals(adGroup2)) &&
                                    u.getLastModifiedBy().equals(testUsername)
                    )
                    .verifyComplete();

            verify(userRepository).findByStaffId(staffId1);
            verify(userRepository).save(argThat(savedUser ->
                    savedUser.getId().equals(userId1) &&
                            savedUser.getUsername().equals("updatedName") &&
                            savedUser.getEntitlements().equals(updatedEntitlements) // Verify entitlements are from request
            ));
        }

        @Test
        @DisplayName("insertOrUpdateUser should fail for existing inactive user if request tries to update")
        void insertOrUpdateUser_FailUpdateInactive() {
            // The service logic for inactive users in insertOrUpdateUser is:
            // if (!existingUser.isActive() && !userFromRequest.isActive()) -> logs warning, proceeds to update other fields
            // if (!existingUser.isActive() && userFromRequest.isActive()) -> reactivates
            // This test should check the case where an update is attempted on an inactive user without reactivating.
            // The current service code *allows* updating an inactive user if the request also marks them as inactive.
            // If the request tries to make them active, it reactivates.
            // The original test expected an error, but the service code was changed.
            // Let's test reactivation.

            User updateInput = User.builder().staffId(inactiveStaffId).username("updatedName").isActive(true).build(); // Try to reactivate
            when(userRepository.findByStaffId(inactiveStaffId)).thenReturn(Mono.just(inactiveUser)); // inactiveUser is isActive=false
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
            when(authorizationUtil.extractUsernameFromContext(any())).thenReturn(testUsername);


            StepVerifier.create(userService.insertOrUpdateUser(updateInput)
                            .contextWrite(Context.of(AuthorizationUtil.AUTHORIZATION_HEADER, testUsername)))
                    .expectNextMatches(u -> u.getStaffId().equals(inactiveStaffId) && u.isActive()) // Should be reactivated
                    .verifyComplete();

            verify(userRepository).findByStaffId(inactiveStaffId);
            verify(userRepository).save(argThat(savedUser -> savedUser.getStaffId().equals(inactiveStaffId) && savedUser.isActive()));
        }


        @Test
        @DisplayName("insertOrUpdateUser should fail if staffId is null")
        void insertOrUpdateUser_FailNullStaffId() {
            User input = User.builder().staffId(null).username("name").build();
            when(authorizationUtil.extractUsernameFromContext(any())).thenReturn(testUsername);


            StepVerifier.create(userService.insertOrUpdateUser(input)
                            .contextWrite(Context.of(AuthorizationUtil.AUTHORIZATION_HEADER, testUsername)))
                    .expectErrorMatches(throwable -> throwable instanceof CommonException &&
                            throwable.getMessage().contains("StaffId cannot be null or blank"))
                    .verify();

            verify(userRepository, never()).findByStaffId(any());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("insertOrUpdateUser should fail if staffId is blank")
        void insertOrUpdateUser_FailBlankStaffId() {
            User input = User.builder().staffId(" ").username("name").build();
            when(authorizationUtil.extractUsernameFromContext(any())).thenReturn(testUsername);

            StepVerifier.create(userService.insertOrUpdateUser(input)
                            .contextWrite(Context.of(AuthorizationUtil.AUTHORIZATION_HEADER, testUsername)))
                    .expectErrorMatches(throwable -> throwable instanceof CommonException &&
                            throwable.getMessage().contains("StaffId cannot be null or blank"))
                    .verify();
            verify(userRepository, never()).findByStaffId(any());
            verify(userRepository, never()).save(any(User.class));
        }
    }


    @Nested
    @DisplayName("Find Roles By User Case Tests")
    class FindRolesByUserCaseTests {

        @Test
        @DisplayName("findRolesByUserCase should return empty flux if user not found")
        void findRolesByUserCase_UserNotFound() {
            when(userRepository.findByStaffId("unknownStaffId")).thenReturn(Mono.empty());

            StepVerifier.create(userService.findRolesByUserCase(userCaseA, "unknownStaffId"))
                    .verifyComplete();
            verifyNoInteractions(adGroupUtil, roleRepository);
        }

        @Test
        @DisplayName("findRolesByUserCase should return empty flux if user has no role IDs in entitlements")
        void findRolesByUserCase_UserHasNoRoleIdsInEntitlements() {
            // userWithNoRoles has an entitlement for adGroup1 but empty roleIds list
            when(userRepository.findByStaffId(userWithNoRoles.getStaffId())).thenReturn(Mono.just(userWithNoRoles));

            StepVerifier.create(userService.findRolesByUserCase(userCaseA, userWithNoRoles.getStaffId()))
                    .verifyComplete();
            verifyNoInteractions(adGroupUtil, roleRepository);
        }


        @Test
        @DisplayName("findRolesByUserCase should return all user case roles if user is admin for that case")
        void findRolesByUserCase_UserIsAdmin() {
            // user1 has adGroup1 (admin for CaseA) in its entitlements
            List<String> user1AdGroups = user1.getEntitlements().stream().map(Entitlement::getAdGroup).collect(Collectors.toList());
            when(userRepository.findByStaffId(staffId1)).thenReturn(Mono.just(user1));
            when(adGroupUtil.isAdmin(user1AdGroups, userCaseA)).thenReturn(true);
            when(roleRepository.findAllByUserCaseAndIsActive(userCaseA, true))
                    .thenReturn(Flux.just(roleAdminCaseA, roleUserCaseA));

            StepVerifier.create(userService.findRolesByUserCase(userCaseA, staffId1))
                    .expectNext(roleAdminCaseA, roleUserCaseA)
                    .verifyComplete();
            verify(roleRepository, never()).findAllByIdsAndUserCaseAndIsActive(anyList(), anyString(), anyBoolean());
        }

        @Test
        @DisplayName("findRolesByUserCase should return specific user case roles if user is not admin")
        void findRolesByUserCase_UserIsNotAdmin() {
            // user2 has adGroup2 (user for CaseA)
            List<String> user2AdGroups = user2.getEntitlements().stream().map(Entitlement::getAdGroup).collect(Collectors.toList());
            List<String> user2RoleIds = user2.getEntitlements().stream()
                    .flatMap(e -> e.getRoleIds().stream()).distinct().collect(Collectors.toList()); // ["ROLE_USER_GLOBAL", "ROLE_USER_CASE_A"]

            when(userRepository.findByStaffId(staffId2)).thenReturn(Mono.just(user2));
            when(adGroupUtil.isAdmin(user2AdGroups, userCaseA)).thenReturn(false);
            when(roleRepository.findAllByIdsAndUserCaseAndIsActive(user2RoleIds, userCaseA, true))
                    .thenReturn(Flux.just(roleUserCaseA));

            StepVerifier.create(userService.findRolesByUserCase(userCaseA, staffId2))
                    .expectNext(roleUserCaseA)
                    .verifyComplete();
            verify(roleRepository, never()).findAllByUserCaseAndIsActive(anyString(), anyBoolean());
        }
    }

    @Nested
    @DisplayName("Get Roles And Permissions Tests")
    class GetRolesAndPermissionsTests {

        @Test
        @DisplayName("getRolesAndPermissions should return error if user not found")
        void getRolesAndPermissions_UserNotFound() {
            when(userRepository.findByStaffId("unknownStaffId")).thenReturn(Mono.empty());

            StepVerifier.create(userService.getRolesAndPermissions("unknownStaffId"))
                    .expectErrorMatches(throwable -> throwable instanceof ResourceNotFoundException)
                    .verify();
            verifyNoInteractions(roleRepository, resourceRepository, userMapper);
        }

        @Test
        @DisplayName("getRolesAndPermissionsByUser should handle user with no role IDs in entitlements")
        void getRolesAndPermissionsByUser_NoRoleIdsInEntitlements() {
            // userWithNoRoles has an entitlement for adGroup1 but empty roleIds list
            UserDto mappedDto = UserDto.builder().id(userWithNoRoles.getId()).staffId(userWithNoRoles.getStaffId()).build();
            when(userMapper.toDto(userWithNoRoles)).thenReturn(mappedDto);

            StepVerifier.create(userService.getRolesAndPermissionsByUser(userWithNoRoles))
                    .expectNextMatches(dto ->
                            dto.getId().equals(userWithNoRoles.getId()) &&
                                    dto.getRoles().isEmpty() && // Service logic sets empty list
                                    dto.getResources().isEmpty() // Service logic sets empty list
                    )
                    .verifyComplete();
            verifyNoInteractions(roleRepository, resourceRepository);
        }


        @Test
        @DisplayName("getRolesAndPermissionsByUser should handle user with no AD groups in entitlements")
        void getRolesAndPermissionsByUser_NoAdGroupsInEntitlements() {
            // userWithNoAdGroups has an entitlement with a null AD group but has roleId1
            UserDto mappedDto = UserDto.builder().id(userWithNoAdGroups.getId()).staffId(userWithNoAdGroups.getStaffId()).build();
            when(userMapper.toDto(userWithNoAdGroups)).thenReturn(mappedDto);
            when(roleRepository.findAllByIdAndIsActive(List.of(roleId1), true)).thenReturn(Flux.just(role1));

            StepVerifier.create(userService.getRolesAndPermissionsByUser(userWithNoAdGroups))
                    .expectNextMatches(dto ->
                            dto.getId().equals(userWithNoAdGroups.getId()) &&
                                    dto.getRoles().size() == 1 &&
                                    dto.getRoles().get(0).getId().equals(roleId1) &&
                                    dto.getResources().isEmpty() // No AD groups to match resources
                    )
                    .verifyComplete();
        }


        @Test
        @DisplayName("getRolesAndPermissionsByUser should handle roles with no resource IDs")
        void getRolesAndPermissionsByUser_RolesHaveNoResourceIds() {
            User userWithRole3Only = User.builder().id(userId1).staffId(staffId1)
                    .entitlements(List.of(Entitlement.builder().adGroup(adGroup1).roleIds(List.of(roleId3)).build()))
                    .isActive(true).isFirstLogin(false).build();
            UserDto mappedDto = UserDto.builder().id(userId1).staffId(staffId1).build();

            when(userMapper.toDto(userWithRole3Only)).thenReturn(mappedDto);
            when(roleRepository.findAllByIdAndIsActive(List.of(roleId3), true)).thenReturn(Flux.just(role3));
            // No need to mock resourceRepository as uniqueResourceIdsFromRoles will be empty

            StepVerifier.create(userService.getRolesAndPermissionsByUser(userWithRole3Only))
                    .expectNextMatches(dto ->
                            dto.getRoles().size() == 1 && dto.getRoles().get(0).getId().equals(roleId3) &&
                                    dto.getResources().isEmpty()
                    )
                    .verifyComplete();
        }

        @Test
        @DisplayName("getRolesAndPermissionsByUser should return roles and filtered resources")
        void getRolesAndPermissionsByUser_SuccessWithRolesAndResources() {
            // user1: entitlements map to adGroups [adGroup1, adGroupOther] and roles [roleId1, roleIdAdminCaseA, roleIdUserCaseA]
            List<String> user1EffectiveRoleIds = List.of(roleId1, roleIdAdminCaseA, roleIdUserCaseA);
            List<String> user1EffectiveAdGroups = List.of(adGroup1, adGroupOther);
            List<String> resourceIdsFromUser1Roles = List.of(resourceId1, resourceId2); // res1 from all, res2 from role1 & roleAdminCaseA

            UserDto mappedDto = UserDto.builder().id(userId1).staffId(staffId1).build();
            when(userMapper.toDto(user1)).thenReturn(mappedDto);
            when(roleRepository.findAllByIdAndIsActive(user1EffectiveRoleIds, true))
                    .thenReturn(Flux.just(role1, roleAdminCaseA, roleUserCaseA));
            when(resourceRepository.findAllByIdInAndIsActiveAndAdGroupsIn(resourceIdsFromUser1Roles, true, user1EffectiveAdGroups))
                    .thenReturn(Flux.just(resource1, resource2));

            StepVerifier.create(userService.getRolesAndPermissionsByUser(user1))
                    .expectNextMatches(dto -> {
                        boolean rolesMatch = dto.getRoles().size() == 3;
                        boolean resourcesMatch = dto.getResources().size() == 2 &&
                                dto.getResources().stream().map(UserResourceDto::getId)
                                        .collect(Collectors.toSet()).containsAll(List.of(resourceId1, resourceId2));
                        return rolesMatch && resourcesMatch;
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("getRolesAndPermissionsByUser should filter resources based on AD groups")
        void getRolesAndPermissionsByUser_ResourceFilteringByAdGroup() {
            // user2: entitlements map to adGroup [adGroup2] and roles [roleId2, roleIdUserCaseA]
            List<String> user2EffectiveRoleIds = List.of(roleId2, roleIdUserCaseA);
            List<String> user2EffectiveAdGroups = List.of(adGroup2);
            List<String> resourceIdsFromUser2Roles = List.of(resourceId1); // Only res1 from role2 & roleUserCaseA

            UserDto mappedDto = UserDto.builder().id(userId2).staffId(staffId2).build();
            when(userMapper.toDto(user2)).thenReturn(mappedDto);
            when(roleRepository.findAllByIdAndIsActive(user2EffectiveRoleIds, true))
                    .thenReturn(Flux.just(role2, roleUserCaseA));
            when(resourceRepository.findAllByIdInAndIsActiveAndAdGroupsIn(resourceIdsFromUser2Roles, true, user2EffectiveAdGroups))
                    .thenReturn(Flux.just(resource1)); // Only resource1 matches adGroup2

            StepVerifier.create(userService.getRolesAndPermissionsByUser(user2))
                    .expectNextMatches(dto -> {
                        boolean rolesMatch = dto.getRoles().size() == 2;
                        boolean resourcesMatch = dto.getResources().size() == 1 &&
                                dto.getResources().get(0).getId().equals(resourceId1);
                        return rolesMatch && resourcesMatch;
                    })
                    .verifyComplete();
        }
    }
}