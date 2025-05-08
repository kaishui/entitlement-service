package com.kaishui.entitlement.service;

import com.kaishui.entitlement.entity.*;
import com.kaishui.entitlement.entity.dto.EntitlementDto; // Ensure this is imported
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

import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

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
    // userDto1 is not directly used for getRolesAndPermissionsByUser assertions,
    // as the service constructs the DTO and populates its complex fields.
    // private UserDto userDto1;

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
        Entitlement entitlementUser1GroupOther = Entitlement.builder().adGroup(adGroupOther).roleIds(List.of(roleIdUserCaseA)).build();
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
                .isActive(false).isFirstLogin(false).entitlements(Collections.emptyList())
                .build();

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
                .entitlements(List.of(Entitlement.builder().adGroup(null).roleIds(List.of(roleId1)).build()))
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

        // DTOs are now simpler as UserMapper only maps basic fields.
        // The service populates entitlements and resources.
        // userDto1 = UserDto.builder().id(userId1).staffId(staffId1).build(); // Example if needed elsewhere
    }

    // ... Other test classes (GetUserTests, UpdateUserTests, DeleteUserTests, ProcessFirstLoginTests, InsertOrUpdateUserTests, FindRolesByUserCaseTests) ...
    // These should remain largely the same, ensuring User objects are built with the 'entitlements' field.

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
        @DisplayName("getRolesAndPermissionsByUser should handle user with no entity entitlements")
        void getRolesAndPermissionsByUser_NoEntityEntitlements() {
            User userWithNoEntityEntitlements = User.builder().id(userId1).staffId(staffId1)
                    .entitlements(Collections.emptyList()) // No entity entitlements
                    .isActive(true).isFirstLogin(false).build();
            // UserMapper.toDto will be called
            UserDto basicDto = UserDto.builder().id(userId1).staffId(staffId1).build();
            when(userMapper.toDto(userWithNoEntityEntitlements)).thenReturn(basicDto);

            StepVerifier.create(userService.getRolesAndPermissionsByUser(userWithNoEntityEntitlements))
                    .expectNextMatches(dto -> {
                        assertThat(dto.getId()).isEqualTo(userId1);
                        assertThat(dto.getEntitlements()).isNotNull().isEmpty();
                        assertThat(dto.getResources()).isNotNull().isEmpty();
                        return true;
                    })
                    .verifyComplete();
            verify(userMapper).toDto(userWithNoEntityEntitlements);
            verifyNoInteractions(roleRepository, resourceRepository);
        }


        @Test
        @DisplayName("getRolesAndPermissionsByUser should handle user with entity entitlements but no role IDs")
        void getRolesAndPermissionsByUser_NoRoleIdsInEntitlements() {
            // userWithNoRoles has an entity Entitlement for adGroup1 but empty roleIds list
            UserDto basicDto = UserDto.builder().id(userWithNoRoles.getId()).staffId(userWithNoRoles.getStaffId()).build();
            when(userMapper.toDto(userWithNoRoles)).thenReturn(basicDto);
            // roleRepository.findAllByIdAndIsActive will be called with an empty list for the entitlement
            when(roleRepository.findAllByIdAndIsActive(Collections.emptyList(), true)).thenReturn(Flux.empty());


            StepVerifier.create(userService.getRolesAndPermissionsByUser(userWithNoRoles))
                    .expectNextMatches(dto -> {
                        assertThat(dto.getId()).isEqualTo(userWithNoRoles.getId());
                        assertThat(dto.getEntitlements()).isNotNull().hasSize(1);
                        EntitlementDto entitlementDto = dto.getEntitlements().get(0);
                        assertThat(entitlementDto.getAdGroup()).isEqualTo(adGroup1);
                        assertThat(entitlementDto.getRoles()).isNotNull().isEmpty();
                        assertThat(dto.getResources()).isNotNull().isEmpty();
                        return true;
                    })
                    .verifyComplete();
            verify(userMapper).toDto(userWithNoRoles);
            // Role repository is called for each entity entitlement's roleIds list.
            // If userWithNoRoles.getEntitlements().get(0).getRoleIds() is empty, it will be called with empty list.
            verify(roleRepository).findAllByIdAndIsActive(Collections.emptyList(), true);
            verifyNoInteractions(resourceRepository); // No roles, so no resources
        }


        @Test
        @DisplayName("getRolesAndPermissionsByUser should handle user with entity entitlements but null AD group")
        void getRolesAndPermissionsByUser_NullAdGroupInEntitlement() {
            // userWithNoAdGroups has an entity Entitlement with a null AD group but has roleId1
            // The service logic in getRolesAndPermissionsByUser filters out entity entitlements with blank AD groups.
            UserDto basicDto = UserDto.builder().id(userWithNoAdGroups.getId()).staffId(userWithNoAdGroups.getStaffId()).build();
            when(userMapper.toDto(userWithNoAdGroups)).thenReturn(basicDto);
            // Since the AD group is null, that entity entitlement will be skipped by the service.
            // No call to roleRepository for that specific entitlement.

            StepVerifier.create(userService.getRolesAndPermissionsByUser(userWithNoAdGroups))
                    .expectNextMatches(dto -> {
                        assertThat(dto.getId()).isEqualTo(userWithNoAdGroups.getId());
                        assertThat(dto.getEntitlements()).isNotNull().isEmpty(); // Entitlement with null AD group is skipped
                        assertThat(dto.getResources()).isNotNull().isEmpty();
                        return true;
                    })
                    .verifyComplete();
            verify(userMapper).toDto(userWithNoAdGroups);
            // Role repository should not be called if the only entitlement has a null AD group
            verifyNoInteractions(roleRepository, resourceRepository);
        }


        @Test
        @DisplayName("getRolesAndPermissionsByUser should handle roles with no resource IDs")
        void getRolesAndPermissionsByUser_RolesHaveNoResourceIds() {
            User userWithRole3Only = User.builder().id(userId1).staffId(staffId1)
                    .entitlements(List.of(Entitlement.builder().adGroup(adGroup1).roleIds(List.of(roleId3)).build()))
                    .isActive(true).isFirstLogin(false).build();
            UserDto basicDto = UserDto.builder().id(userId1).staffId(staffId1).build();

            when(userMapper.toDto(userWithRole3Only)).thenReturn(basicDto);
            when(roleRepository.findAllByIdAndIsActive(List.of(roleId3), true)).thenReturn(Flux.just(role3)); // role3 has no resource IDs
            // resourceRepository.findAllByIdInAndIsActiveAndAdGroupsIn will be called with an empty list of resource IDs

            StepVerifier.create(userService.getRolesAndPermissionsByUser(userWithRole3Only))
                    .expectNextMatches(dto -> {
                        assertThat(dto.getEntitlements()).isNotNull().hasSize(1);
                        EntitlementDto edto = dto.getEntitlements().get(0);
                        assertThat(edto.getAdGroup()).isEqualTo(adGroup1);
                        assertThat(edto.getRoles()).hasSize(1);
                        assertThat(edto.getRoles().get(0).getId()).isEqualTo(roleId3);
                        assertThat(dto.getResources()).isNotNull().isEmpty(); // No resources from role3
                        return true;
                    })
                    .verifyComplete();
            verify(userMapper).toDto(userWithRole3Only);
            verify(roleRepository).findAllByIdAndIsActive(List.of(roleId3), true);
            // Resource repo will be called with empty list if role3.getResourceIds() is empty
            verify(resourceRepository).findAllByIdInAndIsActiveAndAdGroupsIn(Collections.emptyList(), true, List.of(adGroup1));
        }

        @Test
        @DisplayName("getRolesAndPermissionsByUser should return DTO with entitlements and filtered resources")
        void getRolesAndPermissionsByUser_SuccessWithEntitlementsAndResources() {
            // user1 has two entity Entitlements:
            // 1. adGroup1 with roleIds [roleId1, roleIdAdminCaseA]
            // 2. adGroupOther with roleIds [roleIdUserCaseA]
            UserDto basicDto = UserDto.builder().id(userId1).staffId(staffId1).build();
            when(userMapper.toDto(user1)).thenReturn(basicDto);

            // Mock role fetching for each entity entitlement
            when(roleRepository.findAllByIdAndIsActive(List.of(roleId1, roleIdAdminCaseA), true))
                    .thenReturn(Flux.just(role1, roleAdminCaseA));
            when(roleRepository.findAllByIdAndIsActive(List.of(roleIdUserCaseA), true))
                    .thenReturn(Flux.just(roleUserCaseA));

            // All unique resource IDs from these roles: [resourceId1, resourceId2] (from role1, roleAdminCaseA, roleUserCaseA)
            // All unique AD groups from user1's entity entitlements: [adGroup1, adGroupOther]
            List<String> allResourceIds = List.of(resourceId1, resourceId2);
            List<String> allAdGroups = List.of(adGroup1, adGroupOther);
            when(resourceRepository.findAllByIdInAndIsActiveAndAdGroupsIn(allResourceIds, true, allAdGroups))
                    .thenReturn(Flux.just(resource1, resource2)); // Both resources match these AD groups

            StepVerifier.create(userService.getRolesAndPermissionsByUser(user1))
                    .expectNextMatches(dto -> {
                        assertThat(dto.getEntitlements()).isNotNull().hasSize(2);
                        // Check first EntitlementDto
                        Optional<EntitlementDto> edto1Opt = dto.getEntitlements().stream().filter(e -> e.getAdGroup().equals(adGroup1)).findFirst();
                        assertThat(edto1Opt).isPresent();
                        EntitlementDto edto1 = edto1Opt.get();
                        assertThat(edto1.getRoles()).hasSize(2);
                        assertThat(edto1.getRoles().stream().map(Role::getId).collect(Collectors.toSet())).containsExactlyInAnyOrder(roleId1, roleIdAdminCaseA);

                        // Check second EntitlementDto
                        Optional<EntitlementDto> edto2Opt = dto.getEntitlements().stream().filter(e -> e.getAdGroup().equals(adGroupOther)).findFirst();
                        assertThat(edto2Opt).isPresent();
                        EntitlementDto edto2 = edto2Opt.get();
                        assertThat(edto2.getRoles()).hasSize(1);
                        assertThat(edto2.getRoles().get(0).getId()).isEqualTo(roleIdUserCaseA);

                        assertThat(dto.getResources()).hasSize(2);
                        assertThat(dto.getResources().stream().map(UserResourceDto::getId).collect(Collectors.toSet()))
                                .containsExactlyInAnyOrder(resourceId1, resourceId2);
                        return true;
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("getRolesAndPermissionsByUser should filter resources based on AD groups in entitlements")
        void getRolesAndPermissionsByUser_ResourceFilteringByAdGroup() {
            // user2 has one entity Entitlement: adGroup2 with roleIds [roleId2, roleIdUserCaseA]
            UserDto basicDto = UserDto.builder().id(userId2).staffId(staffId2).build();
            when(userMapper.toDto(user2)).thenReturn(basicDto);

            when(roleRepository.findAllByIdAndIsActive(List.of(roleId2, roleIdUserCaseA), true))
                    .thenReturn(Flux.just(role2, roleUserCaseA));

            // All unique resource IDs from these roles: [resourceId1] (from role2, roleUserCaseA)
            // All unique AD groups from user2's entity entitlements: [adGroup2]
            List<String> resourceIdsForUser2 = List.of(resourceId1);
            List<String> adGroupsForUser2 = List.of(adGroup2);
            // resource1 has adGroup2, resource2 does not.
            when(resourceRepository.findAllByIdInAndIsActiveAndAdGroupsIn(resourceIdsForUser2, true, adGroupsForUser2))
                    .thenReturn(Flux.just(resource1));

            StepVerifier.create(userService.getRolesAndPermissionsByUser(user2))
                    .expectNextMatches(dto -> {
                        assertThat(dto.getEntitlements()).isNotNull().hasSize(1);
                        EntitlementDto edto = dto.getEntitlements().get(0);
                        assertThat(edto.getAdGroup()).isEqualTo(adGroup2);
                        assertThat(edto.getRoles()).hasSize(2);
                        assertThat(edto.getRoles().stream().map(Role::getId).collect(Collectors.toSet())).containsExactlyInAnyOrder(roleId2, roleIdUserCaseA);

                        assertThat(dto.getResources()).hasSize(1);
                        assertThat(dto.getResources().get(0).getId()).isEqualTo(resourceId1);
                        return true;
                    })
                    .verifyComplete();
        }
    }
    // Other test classes like GetUserTests, UpdateUserTests, etc.
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
            // This test focuses on insertOrUpdateUser. processFirstLogin is tested separately.
            // The UserController is responsible for calling processFirstLogin after insertOrUpdateUser.
            // So, insertOrUpdateUser should return the user as saved, before processFirstLogin.

            when(userRepository.findByStaffId("newStaff123")).thenReturn(Mono.empty());
            when(userRepository.save(eq(newUserInput))).thenReturn(Mono.just(savedUser)); // Save before first login
            when(authorizationUtil.extractUsernameFromContext(any())).thenReturn(testUsername);


            StepVerifier.create(userService.insertOrUpdateUser(newUserInput)
                            .contextWrite(Context.of(AuthorizationUtil.AUTHORIZATION_HEADER, testUsername)))
                    .expectNextMatches(u ->
                            u.getId().equals(savedUser.getId()) &&
                                    u.isFirstLogin() && // Should still be true, as processFirstLogin is separate
                                    u.getEntitlements().get(0).getAdGroup().equals(adGroup1) &&
                                    u.getEntitlements().get(0).getRoleIds().isEmpty() // Roles are empty initially
                    )
                    .verifyComplete();

            verify(userRepository).findByStaffId("newStaff123");
            verify(userRepository).save(eq(newUserInput)); // Initial save
            // No verification of processFirstLogin's internal calls here, as it's not called by this method directly.
        }

        @Test
        @DisplayName("insertOrUpdateUser should update existing active user with entitlements")
        void insertOrUpdateUser_UpdateExistingActive() {
            List<Entitlement> initialEntitlements = List.of(Entitlement.builder().adGroup(adGroup1).roleIds(List.of(roleId1)).build());
            // Request comes with AD groups, roles within these requested entitlements are typically empty or ignored by merge logic
            // if the AD group already exists (roles are preserved from existing).
            List<Entitlement> requestEntitlements = List.of(
                    Entitlement.builder().adGroup(adGroup1).roleIds(Collections.emptyList()).build(), // Existing AD group
                    Entitlement.builder().adGroup(adGroup2).roleIds(Collections.emptyList()).build()  // New AD group
            );
            // Expected merged entitlements: adGroup1 keeps its roles, adGroup2 is new with empty roles.
            List<Entitlement> expectedMergedEntitlements = List.of(
                    Entitlement.builder().adGroup(adGroup1).roleIds(List.of(roleId1)).build(),
                    Entitlement.builder().adGroup(adGroup2).roleIds(Collections.emptyList()).build()
            );


            User updateInput = User.builder().staffId(staffId1).username("updatedName").email("updated@test.com")
                    .entitlements(requestEntitlements).isActive(true).build();
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
                    .expectNextMatches(u -> {
                        assertThat(u.getId()).isEqualTo(userId1);
                        assertThat(u.getUsername()).isEqualTo("updatedName");
                        assertThat(u.getLastModifiedBy()).isEqualTo(testUsername);
                        assertThat(u.getEntitlements()).isNotNull();
                        // Convert to a map for easier comparison if order doesn't matter
                        Map<String, List<String>> actualEntitlementsMap = u.getEntitlements().stream()
                                .collect(Collectors.toMap(Entitlement::getAdGroup, Entitlement::getRoleIds));
                        Map<String, List<String>> expectedEntitlementsMap = expectedMergedEntitlements.stream()
                                .collect(Collectors.toMap(Entitlement::getAdGroup, Entitlement::getRoleIds));
                        assertThat(actualEntitlementsMap).isEqualTo(expectedEntitlementsMap);
                        return true;
                    })
                    .verifyComplete();

            verify(userRepository).findByStaffId(staffId1);
            verify(userRepository).save(argThat(savedUser ->
                    savedUser.getId().equals(userId1) &&
                            savedUser.getUsername().equals("updatedName") &&
                            // More robust check for entitlements content
                            savedUser.getEntitlements().stream()
                                    .collect(Collectors.toMap(Entitlement::getAdGroup, Entitlement::getRoleIds))
                                    .equals(expectedMergedEntitlements.stream()
                                            .collect(Collectors.toMap(Entitlement::getAdGroup, Entitlement::getRoleIds)))
            ));
        }

        @Test
        @DisplayName("insertOrUpdateUser should reactivate inactive user if request sets active=true")
        void insertOrUpdateUser_ReactivateInactive() {
            User updateInput = User.builder().staffId(inactiveStaffId).username("updatedName").isActive(true).build();
            // inactiveUser is isActive=false
            when(userRepository.findByStaffId(inactiveStaffId)).thenReturn(Mono.just(inactiveUser));
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
            // No need to mock authorizationUtil here as the check happens before context access
            StepVerifier.create(userService.insertOrUpdateUser(input))
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
            // No need to mock authorizationUtil here
            StepVerifier.create(userService.insertOrUpdateUser(input))
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
            // user2 has adGroup2 (user for CaseA) and its entitlements contain roleIdUserCaseA and roleId2
            List<String> user2AdGroups = user2.getEntitlements().stream().map(Entitlement::getAdGroup).collect(Collectors.toList());
            List<String> user2RoleIds = user2.getEntitlements().stream()
                    .filter(e -> e.getRoleIds() != null)
                    .flatMap(e -> e.getRoleIds().stream()).distinct().collect(Collectors.toList());

            when(userRepository.findByStaffId(staffId2)).thenReturn(Mono.just(user2));
            when(adGroupUtil.isAdmin(user2AdGroups, userCaseA)).thenReturn(false);
            when(roleRepository.findAllByIdsAndUserCaseAndIsActive(user2RoleIds, userCaseA, true))
                    .thenReturn(Flux.just(roleUserCaseA)); // Only roleUserCaseA matches the case

            StepVerifier.create(userService.findRolesByUserCase(userCaseA, staffId2))
                    .expectNext(roleUserCaseA)
                    .verifyComplete();
            verify(roleRepository, never()).findAllByUserCaseAndIsActive(anyString(), anyBoolean());
        }
    }
}