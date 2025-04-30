package com.kaishui.entitlement.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils; // Import ReflectionTestUtils

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class) // Enable Mockito annotations
@DisplayName("AdGroupUtil Tests")
class AdGroupUtilTest {

    // Inject the class under test
    @InjectMocks
    private AdGroupUtil adGroupUtil;

    // Define constants for test values to avoid magic strings
    private static final String PREFIX = "AD_";
    private static final String ADMIN_SUFFIX = "ADMIN";
    private static final String MANAGER_SUFFIX = "MANAGER";
    private static final String USER_SUFFIX = "USER";
    private static final String TEST_USER_CASE = "CaseA";

    @BeforeEach
    void setUp() {
        // Manually set the @Value fields before each test
        // Using ReflectionTestUtils to set private fields
        ReflectionTestUtils.setField(adGroupUtil, "adGroupPrefix", PREFIX);
        ReflectionTestUtils.setField(adGroupUtil, "adGroupAdminSuffix", ADMIN_SUFFIX);
        ReflectionTestUtils.setField(adGroupUtil, "adGroupManagerSuffix", MANAGER_SUFFIX);
        ReflectionTestUtils.setField(adGroupUtil, "adGroupUserSuffix", USER_SUFFIX);
    }

    @Nested
    @DisplayName("isAdmin Tests")
    class IsAdminTests {

        @Test
        @DisplayName("Should return true when user has the exact admin group")
        void isAdmin_HasExactAdminGroup_ReturnsTrue() {
            List<String> adGroups = List.of("SomeOtherGroup", PREFIX + TEST_USER_CASE + ADMIN_SUFFIX, "AnotherGroup");
            assertTrue(adGroupUtil.isAdmin(adGroups, TEST_USER_CASE));
        }

        @Test
        @DisplayName("Should return true when user has the admin group with different case")
        void isAdmin_HasAdminGroupDifferentCase_ReturnsTrue() {
            List<String> adGroups = List.of("someothergroup", (PREFIX + TEST_USER_CASE + ADMIN_SUFFIX).toLowerCase());
            assertTrue(adGroupUtil.isAdmin(adGroups, TEST_USER_CASE));
        }

        @Test
        @DisplayName("Should return false when user does not have the admin group")
        void isAdmin_DoesNotHaveAdminGroup_ReturnsFalse() {
            List<String> adGroups = List.of("SomeOtherGroup", PREFIX + TEST_USER_CASE + USER_SUFFIX);
            assertFalse(adGroupUtil.isAdmin(adGroups, TEST_USER_CASE));
        }

        @Test
        @DisplayName("Should return false when adGroups list is empty")
        void isAdmin_EmptyAdGroups_ReturnsFalse() {
            List<String> adGroups = Collections.emptyList();
            assertFalse(adGroupUtil.isAdmin(adGroups, TEST_USER_CASE));
        }

        @Test
        @DisplayName("Should return false when adGroups list is null")
        void isAdmin_NullAdGroups_ReturnsFalse() {
            assertFalse(adGroupUtil.isAdmin(null, TEST_USER_CASE));
        }

        @Test
        @DisplayName("Should return false when userCase is null")
        void isAdmin_NullUserCase_ReturnsFalse() {
            List<String> adGroups = List.of(PREFIX + TEST_USER_CASE + ADMIN_SUFFIX);
            assertFalse(adGroupUtil.isAdmin(adGroups, null));
        }

        @Test
        @DisplayName("Should return false when userCase is blank")
        void isAdmin_BlankUserCase_ReturnsFalse() {
            List<String> adGroups = List.of(PREFIX + TEST_USER_CASE + ADMIN_SUFFIX);
            assertFalse(adGroupUtil.isAdmin(adGroups, "   "));
        }
    }

    @Nested
    @DisplayName("isManager Tests")
    class IsManagerTests {

        @Test
        @DisplayName("Should return true when user has the manager group")
        void isManager_HasManagerGroup_ReturnsTrue() {
            List<String> adGroups = List.of(PREFIX + TEST_USER_CASE + MANAGER_SUFFIX);
            assertTrue(adGroupUtil.isManager(adGroups, TEST_USER_CASE));
        }

        @Test
        @DisplayName("Should return true when user has the manager group with different case")
        void isManager_HasManagerGroupDifferentCase_ReturnsTrue() {
            List<String> adGroups = List.of((PREFIX + TEST_USER_CASE + MANAGER_SUFFIX).toUpperCase());
            assertTrue(adGroupUtil.isManager(adGroups, TEST_USER_CASE));
        }

        @Test
        @DisplayName("Should return false when user does not have the manager group")
        void isManager_DoesNotHaveManagerGroup_ReturnsFalse() {
            List<String> adGroups = List.of(PREFIX + TEST_USER_CASE + ADMIN_SUFFIX);
            assertFalse(adGroupUtil.isManager(adGroups, TEST_USER_CASE));
        }

        @Test
        @DisplayName("Should return false when adGroups list is empty")
        void isManager_EmptyAdGroups_ReturnsFalse() {
            List<String> adGroups = Collections.emptyList();
            assertFalse(adGroupUtil.isManager(adGroups, TEST_USER_CASE));
        }

        @Test
        @DisplayName("Should return false when adGroups list is null")
        void isManager_NullAdGroups_ReturnsFalse() {
            assertFalse(adGroupUtil.isManager(null, TEST_USER_CASE));
        }

        @Test
        @DisplayName("Should return false when userCase is null")
        void isManager_NullUserCase_ReturnsFalse() {
            List<String> adGroups = List.of(PREFIX + TEST_USER_CASE + MANAGER_SUFFIX);
            assertFalse(adGroupUtil.isManager(adGroups, null));
        }
    }

    @Nested
    @DisplayName("isUser Tests")
    class IsUserTests {

        @Test
        @DisplayName("Should return true when user has the user group")
        void isUser_HasUserGroup_ReturnsTrue() {
            List<String> adGroups = List.of(PREFIX + TEST_USER_CASE + USER_SUFFIX);
            assertTrue(adGroupUtil.isUser(adGroups, TEST_USER_CASE));
        }

        @Test
        @DisplayName("Should return true when user has the user group with different case")
        void isUser_HasUserGroupDifferentCase_ReturnsTrue() {
            List<String> adGroups = List.of((PREFIX + TEST_USER_CASE + USER_SUFFIX).toLowerCase());
            assertTrue(adGroupUtil.isUser(adGroups, TEST_USER_CASE));
        }

        @Test
        @DisplayName("Should return false when user does not have the user group")
        void isUser_DoesNotHaveUserGroup_ReturnsFalse() {
            List<String> adGroups = List.of(PREFIX + TEST_USER_CASE + MANAGER_SUFFIX);
            assertFalse(adGroupUtil.isUser(adGroups, TEST_USER_CASE));
        }

        @Test
        @DisplayName("Should return false when adGroups list is empty")
        void isUser_EmptyAdGroups_ReturnsFalse() {
            List<String> adGroups = Collections.emptyList();
            assertFalse(adGroupUtil.isUser(adGroups, TEST_USER_CASE));
        }

        @Test
        @DisplayName("Should return false when adGroups list is null")
        void isUser_NullAdGroups_ReturnsFalse() {
            assertFalse(adGroupUtil.isUser(null, TEST_USER_CASE));
        }

        @Test
        @DisplayName("Should return false when userCase is null")
        void isUser_NullUserCase_ReturnsFalse() {
            List<String> adGroups = List.of(PREFIX + TEST_USER_CASE + USER_SUFFIX);
            assertFalse(adGroupUtil.isUser(adGroups, null));
        }
    }
}