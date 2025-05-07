package com.kaishui.entitlement.service;


import com.kaishui.entitlement.entity.Entitlement; // Import Entitlement
import com.kaishui.entitlement.entity.Role;
import com.kaishui.entitlement.entity.User;
import com.kaishui.entitlement.entity.dto.UserDto;
import com.kaishui.entitlement.repository.RoleRepository;
import com.kaishui.entitlement.repository.UserRepository;
import com.kaishui.entitlement.util.AdGroupUtil;
import com.kaishui.entitlement.util.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.CollectionUtils; // For checking empty collections
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList; // For mutable lists if needed
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService - GetNextLevelUser Tests")
class UserServiceGetNextLevelUserTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private AdGroupUtil adGroupUtil;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    // --- Test Data (Constants for Readability) ---
    private static final String ADMIN_STAFF_ID = "admin001";
    private static final String MANAGER_STAFF_ID = "manager001";
    private static final String USER_STAFF_ID = "user001";
    private static final String TARGET_MANAGER_STAFF_ID_1 = "manager101";
    private static final String TARGET_MANAGER_STAFF_ID_2 = "manager102";
    private static final String TARGET_USER_STAFF_ID_1 = "user101";
    private static final String UNKNOWN_STAFF_ID = "unknownStaffId";
    private static final String NO_GROUP_USER_STAFF_ID = "noGroupUser";
    private static final String TARGET_MANAGER_NO_ROLES_STAFF_ID = "manager301";


    private static final String USER_CASE_A = "CaseA";
    private static final String ADMIN_GROUP_CASE_A = "AD_CaseA_ADMIN";
    private static final String MANAGER_GROUP_CASE_A = "AD_CaseA_MANAGER";
    private static final String USER_GROUP_CASE_A = "AD_CaseA_USER";
    private static final String OTHER_AD_GROUP = "SOME_OTHER_GROUP";

    private static final String ROLE_ID_MANAGER_CASE_A = "ROLE_MANAGER_A";
    private static final String ROLE_ID_USER_CASE_A = "ROLE_USER_A";
    private static final String ROLE_ID_OTHER_CASE = "ROLE_OTHER";

    // --- Test Objects ---
    private User requestingAdmin, requestingManager, requestingUser, requestingUserNoGroup;
    private User targetManager1, targetManager2, targetUser1, targetManagerNoRoles;
    private Role roleManagerCaseA, roleUserCaseA, roleOtherCase;
    private UserDto targetManagerDto1, targetManagerDto2, targetUserDto1, targetManagerDtoNoRoles, requestingUserDtoNoGroup;


    @BeforeEach
    void setUp() {
        // --- Requesting Users ---
        // For requesting users, only their AD groups matter for getNextLevelADGroup.
        // Role IDs within their entitlements can be empty for this test's purpose.
        requestingAdmin = User.builder().staffId(ADMIN_STAFF_ID)
                .entitlements(List.of(Entitlement.builder().adGroup(ADMIN_GROUP_CASE_A).roleIds(Collections.emptyList()).build()))
                .build();
        requestingManager = User.builder().staffId(MANAGER_STAFF_ID)
                .entitlements(List.of(Entitlement.builder().adGroup(MANAGER_GROUP_CASE_A).roleIds(Collections.emptyList()).build()))
                .build();
        requestingUser = User.builder().staffId(USER_STAFF_ID)
                .entitlements(List.of(Entitlement.builder().adGroup(USER_GROUP_CASE_A).roleIds(Collections.emptyList()).build()))
                .build();
        requestingUserNoGroup = User.builder().staffId(NO_GROUP_USER_STAFF_ID)
                .entitlements(List.of(Entitlement.builder().adGroup(OTHER_AD_GROUP).roleIds(Collections.emptyList()).build()))
                .build();


        // --- Target Users ---
        targetManager1 = User.builder().staffId(TARGET_MANAGER_STAFF_ID_1)
                .entitlements(List.of(
                        Entitlement.builder().adGroup(MANAGER_GROUP_CASE_A).roleIds(List.of(ROLE_ID_MANAGER_CASE_A, ROLE_ID_OTHER_CASE)).build()
                ))
                .isActive(true).build();
        targetManager2 = User.builder().staffId(TARGET_MANAGER_STAFF_ID_2)
                .entitlements(List.of(
                        Entitlement.builder().adGroup(MANAGER_GROUP_CASE_A).roleIds(Collections.emptyList()).build()
                ))
                .isActive(true).build();
        targetUser1 = User.builder().staffId(TARGET_USER_STAFF_ID_1)
                .entitlements(List.of(
                        Entitlement.builder().adGroup(USER_GROUP_CASE_A).roleIds(List.of(ROLE_ID_USER_CASE_A)).build()
                ))
                .isActive(true).build();
        targetManagerNoRoles = User.builder().staffId(TARGET_MANAGER_NO_ROLES_STAFF_ID)
                .entitlements(List.of(
                        Entitlement.builder().adGroup(MANAGER_GROUP_CASE_A).roleIds(null).build() // Test with null roleIds list
                ))
                .isActive(true).build();


        // --- Roles ---
        roleManagerCaseA = Role.builder().id(ROLE_ID_MANAGER_CASE_A).roleName("Manager CaseA").userCase(USER_CASE_A).isActive(true).build();
        roleUserCaseA = Role.builder().id(ROLE_ID_USER_CASE_A).roleName("User CaseA").userCase(USER_CASE_A).isActive(true).build();
        roleOtherCase = Role.builder().id(ROLE_ID_OTHER_CASE).roleName("Other Case Role").userCase("CaseB").isActive(true).build();

        // --- DTOs (basic mapping simulation) ---
        targetManagerDto1 = new UserDto();
        targetManagerDto1.setStaffId(TARGET_MANAGER_STAFF_ID_1);
        targetManagerDto2 = new UserDto();
        targetManagerDto2.setStaffId(TARGET_MANAGER_STAFF_ID_2);
        targetUserDto1 = new UserDto();
        targetUserDto1.setStaffId(TARGET_USER_STAFF_ID_1);
        targetManagerDtoNoRoles = new UserDto();
        targetManagerDtoNoRoles.setStaffId(TARGET_MANAGER_NO_ROLES_STAFF_ID);
        requestingUserDtoNoGroup = UserDto.builder().staffId(NO_GROUP_USER_STAFF_ID).build();
    }

    // Helper method to extract AD groups from entitlements for mocking AdGroupUtil
    private List<String> extractAdGroups(User user) {
        if (user == null || CollectionUtils.isEmpty(user.getEntitlements())) {
            return Collections.emptyList();
        }
        return user.getEntitlements().stream()
                .map(Entitlement::getAdGroup)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    // Helper method to extract Role IDs from entitlements for mocking RoleRepository
    private List<String> extractRoleIds(User user) {
        if (user == null || CollectionUtils.isEmpty(user.getEntitlements())) {
            return Collections.emptyList();
        }
        return user.getEntitlements().stream()
                .filter(e -> !CollectionUtils.isEmpty(e.getRoleIds()))
                .flatMap(e -> e.getRoleIds().stream())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }


    @Nested
    @DisplayName("Error and Empty Scenarios")
    class ErrorAndEmptyScenarios {

        @Test
        @DisplayName("Should return empty Flux when requesting user is not found")
        void getNextLevelUser_RequestingUserNotFound() {
            when(userRepository.findByStaffId(UNKNOWN_STAFF_ID)).thenReturn(Mono.empty());

            StepVerifier.create(userService.getNextLevelUser(USER_CASE_A, UNKNOWN_STAFF_ID))
                    .verifyComplete();

            verify(userRepository).findByStaffId(UNKNOWN_STAFF_ID);
            verifyNoInteractions(adGroupUtil, roleRepository, userMapper);
        }

        @Test
        @DisplayName("Should return empty Flux when next level AD group cannot be determined")
        void getNextLevelUser_CannotDetermineNextLevelGroup() {
            when(userRepository.findByStaffId(NO_GROUP_USER_STAFF_ID)).thenReturn(Mono.just(requestingUserNoGroup));
            when(adGroupUtil.getNextLevelADGroup(USER_CASE_A, extractAdGroups(requestingUserNoGroup))).thenReturn("");

            StepVerifier.create(userService.getNextLevelUser(USER_CASE_A, NO_GROUP_USER_STAFF_ID))
                    .verifyComplete();

            verify(userRepository).findByStaffId(NO_GROUP_USER_STAFF_ID);
            verify(adGroupUtil).getNextLevelADGroup(USER_CASE_A, extractAdGroups(requestingUserNoGroup));
            verify(userRepository, never()).findByAdGroupAndIsActive(anyString(), anyBoolean());
            verifyNoInteractions(roleRepository, userMapper);
        }

        @Test
        @DisplayName("Should return empty Flux when no target users are found in the next level group")
        void getNextLevelUser_NoTargetUsersFound() {
            when(userRepository.findByStaffId(ADMIN_STAFF_ID)).thenReturn(Mono.just(requestingAdmin));
            when(adGroupUtil.getNextLevelADGroup(USER_CASE_A, extractAdGroups(requestingAdmin))).thenReturn(MANAGER_GROUP_CASE_A);
            when(userRepository.findByAdGroupAndIsActive(MANAGER_GROUP_CASE_A, true)).thenReturn(Flux.empty());

            StepVerifier.create(userService.getNextLevelUser(USER_CASE_A, ADMIN_STAFF_ID))
                    .verifyComplete();

            verify(userRepository).findByStaffId(ADMIN_STAFF_ID);
            verify(adGroupUtil).getNextLevelADGroup(USER_CASE_A, extractAdGroups(requestingAdmin));
            verify(userRepository).findByAdGroupAndIsActive(MANAGER_GROUP_CASE_A, true);
            verifyNoInteractions(roleRepository, userMapper);
        }

        @Test
        @DisplayName("Should return DTOs with empty roles when target users have no role IDs in entitlements")
        void getNextLevelUser_TargetUsersHaveNoRoleIdsInEntitlements() {
            when(userRepository.findByStaffId(ADMIN_STAFF_ID)).thenReturn(Mono.just(requestingAdmin));
            when(adGroupUtil.getNextLevelADGroup(USER_CASE_A, extractAdGroups(requestingAdmin))).thenReturn(MANAGER_GROUP_CASE_A);
            // targetManagerNoRoles is set up with an entitlement having null roleIds
            when(userRepository.findByAdGroupAndIsActive(MANAGER_GROUP_CASE_A, true)).thenReturn(Flux.just(targetManagerNoRoles));
            when(userMapper.toDto(targetManagerNoRoles)).thenReturn(targetManagerDtoNoRoles);
            // Role repository should not be called if extractRoleIds(targetManagerNoRoles) is empty

            StepVerifier.create(userService.getNextLevelUser(USER_CASE_A, ADMIN_STAFF_ID))
                    .expectNextMatches(dto -> {
                        assertThat(dto.getStaffId()).isEqualTo(TARGET_MANAGER_NO_ROLES_STAFF_ID);
                        assertThat(dto.getRoles()).isNotNull().isEmpty();
                        return true;
                    })
                    .verifyComplete();

            verify(userRepository).findByStaffId(ADMIN_STAFF_ID);
            verify(adGroupUtil).getNextLevelADGroup(USER_CASE_A, extractAdGroups(requestingAdmin));
            verify(userRepository).findByAdGroupAndIsActive(MANAGER_GROUP_CASE_A, true);
            verify(userMapper).toDto(targetManagerNoRoles);
            verifyNoInteractions(roleRepository); // Because targetManagerNoRoles has no actual role IDs
        }
    }

    @Nested
    @DisplayName("Successful Scenarios with Role Fetching")
    class SuccessScenarios {

        @Test
        @DisplayName("Should return DTOs with empty roles when target users roles do not match user case")
        void getNextLevelUser_TargetUsersRolesDoNotMatchUserCase() {
            // targetManager1 has roleIdManagerCaseA and roleIdOtherCase in its entitlement
            List<String> targetManager1RoleIds = extractRoleIds(targetManager1); // [ROLE_MANAGER_A, ROLE_OTHER]

            when(userRepository.findByStaffId(ADMIN_STAFF_ID)).thenReturn(Mono.just(requestingAdmin));
            when(adGroupUtil.getNextLevelADGroup(USER_CASE_A, extractAdGroups(requestingAdmin))).thenReturn(MANAGER_GROUP_CASE_A);
            when(userRepository.findByAdGroupAndIsActive(MANAGER_GROUP_CASE_A, true)).thenReturn(Flux.just(targetManager1));
            when(userMapper.toDto(targetManager1)).thenReturn(targetManagerDto1);
            // Mock role repo to return empty because no roles match the *specific* userCase "CaseA" AND the IDs
            // The service will call with targetManager1RoleIds
            when(roleRepository.findAllByIdsAndUserCaseAndIsActive(
                    argThat(list -> list.containsAll(targetManager1RoleIds) && targetManager1RoleIds.containsAll(list)), // Content check
                    eq(USER_CASE_A),
                    eq(true)))
                    .thenReturn(Flux.empty());

            StepVerifier.create(userService.getNextLevelUser(USER_CASE_A, ADMIN_STAFF_ID))
                    .expectNextMatches(dto -> {
                        assertThat(dto.getStaffId()).isEqualTo(TARGET_MANAGER_STAFF_ID_1);
                        assertThat(dto.getRoles()).isNotNull().isEmpty();
                        return true;
                    })
                    .verifyComplete();

            verify(roleRepository).findAllByIdsAndUserCaseAndIsActive(
                    argThat(list -> list.containsAll(targetManager1RoleIds) && targetManager1RoleIds.containsAll(list)),
                    eq(USER_CASE_A),
                    eq(true));
        }

        @Test
        @DisplayName("Should return DTOs with correct user case roles (Admin -> Manager)")
        void getNextLevelUser_AdminToManager_Success() {
            // targetManager1 has [ROLE_MANAGER_A, ROLE_OTHER]
            // targetManager2 has []
            // The service will collect all unique role IDs from both: [ROLE_MANAGER_A, ROLE_OTHER]
            Set<String> allTargetRoleIdsSet = extractRoleIds(targetManager1).stream().collect(Collectors.toSet());
            allTargetRoleIdsSet.addAll(extractRoleIds(targetManager2));
            List<String> allTargetRoleIdsList = new ArrayList<>(allTargetRoleIdsSet);


            when(userRepository.findByStaffId(ADMIN_STAFF_ID)).thenReturn(Mono.just(requestingAdmin));
            when(adGroupUtil.getNextLevelADGroup(USER_CASE_A, extractAdGroups(requestingAdmin))).thenReturn(MANAGER_GROUP_CASE_A);
            when(userRepository.findByAdGroupAndIsActive(MANAGER_GROUP_CASE_A, true)).thenReturn(Flux.just(targetManager1, targetManager2));
            when(userMapper.toDto(targetManager1)).thenReturn(targetManagerDto1);
            when(userMapper.toDto(targetManager2)).thenReturn(targetManagerDto2);

            // Mock role repo to return only the role matching the userCaseA from the combined list
            when(roleRepository.findAllByIdsAndUserCaseAndIsActive(
                    argThat(list -> list.containsAll(allTargetRoleIdsList) && allTargetRoleIdsList.containsAll(list)),
                    eq(USER_CASE_A),
                    eq(true)))
                    .thenReturn(Flux.just(roleManagerCaseA)); // Only roleManagerCaseA matches userCaseA

            StepVerifier.create(userService.getNextLevelUser(USER_CASE_A, ADMIN_STAFF_ID))
                    .expectNextMatches(dto -> { // targetManager1 DTO
                        assertThat(dto.getStaffId()).isEqualTo(TARGET_MANAGER_STAFF_ID_1);
                        assertThat(dto.getRoles()).hasSize(1);
                        assertThat(dto.getRoles().get(0).getId()).isEqualTo(ROLE_ID_MANAGER_CASE_A);
                        return true;
                    })
                    .expectNextMatches(dto -> { // targetManager2 DTO
                        assertThat(dto.getStaffId()).isEqualTo(TARGET_MANAGER_STAFF_ID_2);
                        assertThat(dto.getRoles()).isNotNull().isEmpty();
                        return true;
                    })
                    .verifyComplete();

            verify(roleRepository).findAllByIdsAndUserCaseAndIsActive(
                    argThat(list -> list.containsAll(allTargetRoleIdsList) && allTargetRoleIdsList.containsAll(list)),
                    eq(USER_CASE_A),
                    eq(true));
        }

        @Test
        @DisplayName("Should return DTOs with correct user case roles (Manager -> User)")
        void getNextLevelUser_ManagerToUser_Success() {
            // targetUser1 has [ROLE_USER_A]
            List<String> targetUser1RoleIds = extractRoleIds(targetUser1);

            when(userRepository.findByStaffId(MANAGER_STAFF_ID)).thenReturn(Mono.just(requestingManager));
            when(adGroupUtil.getNextLevelADGroup(USER_CASE_A, extractAdGroups(requestingManager))).thenReturn(USER_GROUP_CASE_A);
            when(userRepository.findByAdGroupAndIsActive(USER_GROUP_CASE_A, true)).thenReturn(Flux.just(targetUser1));
            when(userMapper.toDto(targetUser1)).thenReturn(targetUserDto1);
            // Mock role repo to return the matching role
            when(roleRepository.findAllByIdsAndUserCaseAndIsActive(
                    argThat(list -> list.containsAll(targetUser1RoleIds) && targetUser1RoleIds.containsAll(list)),
                    eq(USER_CASE_A),
                    eq(true)))
                    .thenReturn(Flux.just(roleUserCaseA));

            StepVerifier.create(userService.getNextLevelUser(USER_CASE_A, MANAGER_STAFF_ID))
                    .expectNextMatches(dto -> {
                        assertThat(dto.getStaffId()).isEqualTo(TARGET_USER_STAFF_ID_1);
                        assertThat(dto.getRoles()).hasSize(1);
                        assertThat(dto.getRoles().get(0).getId()).isEqualTo(ROLE_ID_USER_CASE_A);
                        return true;
                    })
                    .verifyComplete();

            verify(roleRepository).findAllByIdsAndUserCaseAndIsActive(
                    argThat(list -> list.containsAll(targetUser1RoleIds) && targetUser1RoleIds.containsAll(list)),
                    eq(USER_CASE_A),
                    eq(true));
        }

        @Test
        @DisplayName("Should return DTOs with correct user case roles (User -> Manager)")
        void getNextLevelUser_UserToManager_Success() {
            // targetManager1 has [ROLE_MANAGER_A, ROLE_OTHER]
            List<String> targetManager1RoleIds = extractRoleIds(targetManager1);

            when(userRepository.findByStaffId(USER_STAFF_ID)).thenReturn(Mono.just(requestingUser));
            when(adGroupUtil.getNextLevelADGroup(USER_CASE_A, extractAdGroups(requestingUser))).thenReturn(MANAGER_GROUP_CASE_A);
            when(userRepository.findByAdGroupAndIsActive(MANAGER_GROUP_CASE_A, true)).thenReturn(Flux.just(targetManager1));
            when(userMapper.toDto(targetManager1)).thenReturn(targetManagerDto1);
            // Mock role repo to return only the role matching the userCaseA
            when(roleRepository.findAllByIdsAndUserCaseAndIsActive(
                    argThat(list -> list.containsAll(targetManager1RoleIds) && targetManager1RoleIds.containsAll(list)),
                    eq(USER_CASE_A),
                    eq(true)))
                    .thenReturn(Flux.just(roleManagerCaseA));

            StepVerifier.create(userService.getNextLevelUser(USER_CASE_A, USER_STAFF_ID))
                    .expectNextMatches(dto -> {
                        assertThat(dto.getStaffId()).isEqualTo(TARGET_MANAGER_STAFF_ID_1);
                        assertThat(dto.getRoles()).hasSize(1);
                        assertThat(dto.getRoles().get(0).getId()).isEqualTo(ROLE_ID_MANAGER_CASE_A);
                        return true;
                    })
                    .verifyComplete();

            verify(roleRepository).findAllByIdsAndUserCaseAndIsActive(
                    argThat(list -> list.containsAll(targetManager1RoleIds) && targetManager1RoleIds.containsAll(list)),
                    eq(USER_CASE_A),
                    eq(true));
        }
    }
}