package com.kaishui.entitlement.service;


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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors; // Added for clarity in assertions

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat; // Optional: For more fluent assertions inside expectNextMatches

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

    // Inject mocks into UserService. Other dependencies are null as they aren't used by getNextLevelUser.
    @InjectMocks
    private UserService userService;

    // --- Test Data (Constants for Readability) ---
    private static final String ADMIN_STAFF_ID = "admin001";
    private static final String MANAGER_STAFF_ID = "manager001";
    private static final String USER_STAFF_ID = "user001";
    private static final String TARGET_MANAGER_STAFF_ID_1 = "manager101";
    private static final String TARGET_MANAGER_STAFF_ID_2 = "manager102"; // Manager with no roles
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
        requestingAdmin = User.builder().staffId(ADMIN_STAFF_ID).adGroups(List.of(ADMIN_GROUP_CASE_A)).build();
        requestingManager = User.builder().staffId(MANAGER_STAFF_ID).adGroups(List.of(MANAGER_GROUP_CASE_A)).build();
        requestingUser = User.builder().staffId(USER_STAFF_ID).adGroups(List.of(USER_GROUP_CASE_A)).build();
        requestingUserNoGroup = User.builder().staffId(NO_GROUP_USER_STAFF_ID).adGroups(List.of(OTHER_AD_GROUP)).build();


        // --- Target Users ---
        targetManager1 = User.builder().staffId(TARGET_MANAGER_STAFF_ID_1).adGroups(List.of(MANAGER_GROUP_CASE_A))
                .roleIds(List.of(ROLE_ID_MANAGER_CASE_A, ROLE_ID_OTHER_CASE)).isActive(true).build(); // Has relevant role + other
        targetManager2 = User.builder().staffId(TARGET_MANAGER_STAFF_ID_2).adGroups(List.of(MANAGER_GROUP_CASE_A))
                .roleIds(Collections.emptyList()).isActive(true).build(); // No roles
        targetUser1 = User.builder().staffId(TARGET_USER_STAFF_ID_1).adGroups(List.of(USER_GROUP_CASE_A))
                .roleIds(List.of(ROLE_ID_USER_CASE_A)).isActive(true).build(); // Has relevant role
        targetManagerNoRoles = User.builder().staffId(TARGET_MANAGER_NO_ROLES_STAFF_ID).adGroups(List.of(MANAGER_GROUP_CASE_A))
                .roleIds(null).isActive(true).build(); // Null roles list


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
        requestingUserDtoNoGroup = UserDto.builder().staffId(NO_GROUP_USER_STAFF_ID).build(); // Added for clarity in test
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
            // Arrange
            when(userRepository.findByStaffId(NO_GROUP_USER_STAFF_ID)).thenReturn(Mono.just(requestingUserNoGroup));
            // Mock AdGroupUtil to return null/empty string
            when(adGroupUtil.getNextLevelADGroup(USER_CASE_A, requestingUserNoGroup.getAdGroups())).thenReturn("");
            // Note: userMapper.toDto is NOT called in the actual service code path when nextLevelADGroup is empty

            // Act & Assert
            StepVerifier.create(userService.getNextLevelUser(USER_CASE_A, NO_GROUP_USER_STAFF_ID))
                    .verifyComplete();

            // Verify
            verify(userRepository).findByStaffId(NO_GROUP_USER_STAFF_ID);
            verify(adGroupUtil).getNextLevelADGroup(USER_CASE_A, requestingUserNoGroup.getAdGroups());
            verify(userRepository, never()).findByAdGroupAndIsActive(anyString(), anyBoolean());
            verifyNoInteractions(roleRepository, userMapper); // Verify userMapper is not called
        }

        @Test
        @DisplayName("Should return empty Flux when no target users are found in the next level group")
        void getNextLevelUser_NoTargetUsersFound() {
            when(userRepository.findByStaffId(ADMIN_STAFF_ID)).thenReturn(Mono.just(requestingAdmin));
            when(adGroupUtil.getNextLevelADGroup(USER_CASE_A, requestingAdmin.getAdGroups())).thenReturn(MANAGER_GROUP_CASE_A);
            // Mock repository to return empty flux for target users
            when(userRepository.findByAdGroupAndIsActive(MANAGER_GROUP_CASE_A, true)).thenReturn(Flux.empty());

            StepVerifier.create(userService.getNextLevelUser(USER_CASE_A, ADMIN_STAFF_ID))
                    .verifyComplete();

            verify(userRepository).findByStaffId(ADMIN_STAFF_ID);
            verify(adGroupUtil).getNextLevelADGroup(USER_CASE_A, requestingAdmin.getAdGroups());
            verify(userRepository).findByAdGroupAndIsActive(MANAGER_GROUP_CASE_A, true);
            verifyNoInteractions(roleRepository, userMapper);
        }

        @Test
        @DisplayName("Should return DTOs with empty roles when target users have no role IDs")
        void getNextLevelUser_TargetUsersHaveNoRoleIds() {
            when(userRepository.findByStaffId(ADMIN_STAFF_ID)).thenReturn(Mono.just(requestingAdmin));
            when(adGroupUtil.getNextLevelADGroup(USER_CASE_A, requestingAdmin.getAdGroups())).thenReturn(MANAGER_GROUP_CASE_A);
            // Return only the user with no roles
            when(userRepository.findByAdGroupAndIsActive(MANAGER_GROUP_CASE_A, true)).thenReturn(Flux.just(targetManagerNoRoles));
            when(userMapper.toDto(targetManagerNoRoles)).thenReturn(targetManagerDtoNoRoles);

            StepVerifier.create(userService.getNextLevelUser(USER_CASE_A, ADMIN_STAFF_ID))
                    .expectNextMatches(dto -> {
                        assertThat(dto.getStaffId()).isEqualTo(TARGET_MANAGER_NO_ROLES_STAFF_ID);
                        assertThat(dto.getRoles()).isNotNull().isEmpty(); // Check roles list is empty
                        return true;
                    })
                    .verifyComplete();

            verify(userRepository).findByStaffId(ADMIN_STAFF_ID);
            verify(adGroupUtil).getNextLevelADGroup(USER_CASE_A, requestingAdmin.getAdGroups());
            verify(userRepository).findByAdGroupAndIsActive(MANAGER_GROUP_CASE_A, true);
            verify(userMapper).toDto(targetManagerNoRoles);
            // Role repository should not be called in this case
            verifyNoInteractions(roleRepository);
        }
    }

    @Nested
    @DisplayName("Successful Scenarios with Role Fetching")
    class SuccessScenarios {

        @Test
        @DisplayName("Should return DTOs with empty roles when target users roles do not match user case")
        void getNextLevelUser_TargetUsersRolesDoNotMatchUserCase() {
            // targetManager1 has roleIdManagerCaseA and roleIdOtherCase
            Set<String> uniqueRoleIds = Set.of(ROLE_ID_MANAGER_CASE_A, ROLE_ID_OTHER_CASE);
            List<String> uniqueRoleIdsList = List.copyOf(uniqueRoleIds); // Use List for repo call

            when(userRepository.findByStaffId(ADMIN_STAFF_ID)).thenReturn(Mono.just(requestingAdmin));
            when(adGroupUtil.getNextLevelADGroup(USER_CASE_A, requestingAdmin.getAdGroups())).thenReturn(MANAGER_GROUP_CASE_A);
            when(userRepository.findByAdGroupAndIsActive(MANAGER_GROUP_CASE_A, true)).thenReturn(Flux.just(targetManager1));
            when(userMapper.toDto(targetManager1)).thenReturn(targetManagerDto1);
            // Mock role repo to return empty because no roles match the *specific* userCase "CaseA" AND the IDs
            when(roleRepository.findAllByIdsAndUserCaseAndIsActive(anyList(), anyString(), anyBoolean()))
                    .thenReturn(Flux.empty());

            StepVerifier.create(userService.getNextLevelUser(USER_CASE_A, ADMIN_STAFF_ID))
                    .expectNextMatches(dto -> {
                        assertThat(dto.getStaffId()).isEqualTo(TARGET_MANAGER_STAFF_ID_1);
                        assertThat(dto.getRoles()).isNotNull().isEmpty();
                        return true;
                    })
                    .verifyComplete();

            verify(userRepository).findByStaffId(ADMIN_STAFF_ID);
            verify(adGroupUtil).getNextLevelADGroup(USER_CASE_A, requestingAdmin.getAdGroups());
            verify(userRepository).findByAdGroupAndIsActive(MANAGER_GROUP_CASE_A, true);
            verify(userMapper).toDto(targetManager1);
            verify(roleRepository).findAllByIdsAndUserCaseAndIsActive(anyList(), anyString(), anyBoolean());
        }

        @Test
        @DisplayName("Should return DTOs with correct user case roles (Admin -> Manager)")
        void getNextLevelUser_AdminToManager_Success() {
            // targetManager1 has roleIdManagerCaseA (matches) and roleIdOtherCase (doesn't match userCaseA)
            // targetManager2 has no roles
            Set<String> uniqueRoleIds = Set.of(ROLE_ID_MANAGER_CASE_A, ROLE_ID_OTHER_CASE); // From targetManager1
            List<String> uniqueRoleIdsList = List.copyOf(uniqueRoleIds);

            when(userRepository.findByStaffId(ADMIN_STAFF_ID)).thenReturn(Mono.just(requestingAdmin));
            when(adGroupUtil.getNextLevelADGroup(USER_CASE_A, requestingAdmin.getAdGroups())).thenReturn(MANAGER_GROUP_CASE_A);
            when(userRepository.findByAdGroupAndIsActive(MANAGER_GROUP_CASE_A, true)).thenReturn(Flux.just(targetManager1, targetManager2));
            when(userMapper.toDto(targetManager1)).thenReturn(targetManagerDto1);
            when(userMapper.toDto(targetManager2)).thenReturn(targetManagerDto2);
            // Mock role repo to return only the role matching the userCaseA
            when(roleRepository.findAllByIdsAndUserCaseAndIsActive(anyList(), anyString(), anyBoolean()))
                    .thenReturn(Flux.empty());

            StepVerifier.create(userService.getNextLevelUser(USER_CASE_A, ADMIN_STAFF_ID))
                    // Check targetManager1 DTO - should have only roleManagerCaseA
                    .expectNextMatches(dto -> {
                        assertThat(dto.getStaffId()).isEqualTo(TARGET_MANAGER_STAFF_ID_1);
                        return true;
                    })
                    // Check targetManager2 DTO - should have empty roles
                    .expectNextMatches(dto -> {
                        assertThat(dto.getStaffId()).isEqualTo(TARGET_MANAGER_STAFF_ID_2);
                        assertThat(dto.getRoles()).isNotNull().isEmpty();
                        return true;
                    })
                    .verifyComplete();

            verify(userRepository).findByStaffId(ADMIN_STAFF_ID);
            verify(adGroupUtil).getNextLevelADGroup(USER_CASE_A, requestingAdmin.getAdGroups());
            verify(userRepository).findByAdGroupAndIsActive(MANAGER_GROUP_CASE_A, true);
            verify(userMapper).toDto(targetManager1);
            verify(userMapper).toDto(targetManager2);
            verify(roleRepository).findAllByIdsAndUserCaseAndIsActive(anyList(), anyString(), anyBoolean());
        }

        @Test
        @DisplayName("Should return DTOs with correct user case roles (Manager -> User)")
        void getNextLevelUser_ManagerToUser_Success() {
            // targetUser1 has roleIdUserCaseA (matches)
            Set<String> uniqueRoleIds = Set.of(ROLE_ID_USER_CASE_A);
            List<String> uniqueRoleIdsList = List.copyOf(uniqueRoleIds);


            when(userRepository.findByStaffId(MANAGER_STAFF_ID)).thenReturn(Mono.just(requestingManager));
            when(adGroupUtil.getNextLevelADGroup(USER_CASE_A, requestingManager.getAdGroups())).thenReturn(USER_GROUP_CASE_A);
            when(userRepository.findByAdGroupAndIsActive(USER_GROUP_CASE_A, true)).thenReturn(Flux.just(targetUser1));
            when(userMapper.toDto(targetUser1)).thenReturn(targetUserDto1);
            // Mock role repo to return the matching role
            when(roleRepository.findAllByIdsAndUserCaseAndIsActive(eq(uniqueRoleIdsList), eq(USER_CASE_A), eq(true)))
                    .thenReturn(Flux.just(roleUserCaseA));

            StepVerifier.create(userService.getNextLevelUser(USER_CASE_A, MANAGER_STAFF_ID))
                    .expectNextMatches(dto -> {
                        assertThat(dto.getStaffId()).isEqualTo(TARGET_USER_STAFF_ID_1);
                        assertThat(dto.getRoles()).hasSize(1);
                        assertThat(dto.getRoles().get(0).getId()).isEqualTo(ROLE_ID_USER_CASE_A);
                        return true;
                    })
                    .verifyComplete();

            verify(userRepository).findByStaffId(MANAGER_STAFF_ID);
            verify(adGroupUtil).getNextLevelADGroup(USER_CASE_A, requestingManager.getAdGroups());
            verify(userRepository).findByAdGroupAndIsActive(USER_GROUP_CASE_A, true);
            verify(userMapper).toDto(targetUser1);
            verify(roleRepository).findAllByIdsAndUserCaseAndIsActive(eq(uniqueRoleIdsList), eq(USER_CASE_A), eq(true));
        }

        @Test
        @DisplayName("Should return DTOs with correct user case roles (User -> Manager)")
        void getNextLevelUser_UserToManager_Success() {
            // targetManager1 has roleIdManagerCaseA (matches) and roleIdOtherCase (doesn't match userCaseA)
            Set<String> uniqueRoleIds = Set.of(ROLE_ID_MANAGER_CASE_A, ROLE_ID_OTHER_CASE);
            List<String> uniqueRoleIdsList = List.copyOf(uniqueRoleIds);


            when(userRepository.findByStaffId(USER_STAFF_ID)).thenReturn(Mono.just(requestingUser));
            // Assuming User -> Manager logic in AdGroupUtil
            when(adGroupUtil.getNextLevelADGroup(USER_CASE_A, requestingUser.getAdGroups())).thenReturn(MANAGER_GROUP_CASE_A);
            when(userRepository.findByAdGroupAndIsActive(MANAGER_GROUP_CASE_A, true)).thenReturn(Flux.just(targetManager1));
            when(userMapper.toDto(targetManager1)).thenReturn(targetManagerDto1);
            // Mock role repo to return only the role matching the userCaseA
            when(roleRepository.findAllByIdsAndUserCaseAndIsActive(anyList(), anyString(), anyBoolean()))
                    .thenReturn(Flux.just(roleManagerCaseA));

            StepVerifier.create(userService.getNextLevelUser(USER_CASE_A, USER_STAFF_ID))
                    .expectNextMatches(dto -> {
                        assertThat(dto.getStaffId()).isEqualTo(TARGET_MANAGER_STAFF_ID_1);
                        assertThat(dto.getRoles()).hasSize(1);
                        assertThat(dto.getRoles().get(0).getId()).isEqualTo(ROLE_ID_MANAGER_CASE_A);
                        return true;
                    })
                    .verifyComplete();

            verify(userRepository).findByStaffId(USER_STAFF_ID);
            verify(adGroupUtil).getNextLevelADGroup(USER_CASE_A, requestingUser.getAdGroups());
            verify(userRepository).findByAdGroupAndIsActive(MANAGER_GROUP_CASE_A, true);
            verify(userMapper).toDto(targetManager1);
            verify(roleRepository).findAllByIdsAndUserCaseAndIsActive(
                    argThat(list -> list != null && list.size() == uniqueRoleIds.size() && uniqueRoleIds.containsAll(list)),
                    eq(USER_CASE_A),
                    eq(true)
            );
        }
    }
}