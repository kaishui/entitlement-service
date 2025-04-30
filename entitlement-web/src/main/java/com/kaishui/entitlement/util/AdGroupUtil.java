package com.kaishui.entitlement.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List; // Import List

@Slf4j
@Component
public class AdGroupUtil {

    // Corrected @Value annotations to match the YAML structure
    @Value("${ad.group.prefi}") // Corrected path
    private String adGroupPrefix; // Renamed for clarity

    @Value("${ad.group.suffix.admin}") // Corrected path
    private String adGroupAdminSuffix; // Renamed for clarity

    @Value("${ad.group.suffix.manager}") // Corrected path
    private String adGroupManagerSuffix; // Renamed for clarity

    @Value("${ad.group.suffix.user}") // Corrected path
    private String adGroupUserSuffix; // Renamed for clarity

    /**
     * Checks if the user belongs to the admin group for a specific user case.
     * The expected format is prefix + userCase + adminSuffix (e.g., "AD_CaseA_ADMIN").
     *
     * @param adGroups The list of AD groups the user belongs to.
     * @param userCase The specific user case to check against.
     * @return true if the user has the admin group for the given user case, false otherwise.
     */
    public boolean isAdmin(List<String> adGroups, String userCase) { // Changed parameter type to List<String>
        if (adGroups == null || adGroups.isEmpty() || userCase == null || userCase.isBlank()) {
            return false; // Handle null or empty inputs gracefully
        }
        // Construct the expected admin group name based on configuration
        String expectedAdminGroup = adGroupPrefix + userCase + adGroupAdminSuffix;
        log.debug("Checking for admin group: {}", expectedAdminGroup); // Added debug log

        // Use stream API for cleaner checking
        return adGroups.stream()
                .anyMatch(expectedAdminGroup::equalsIgnoreCase); // Case-insensitive comparison is often safer
    }

    // You might want similar methods for manager and user roles:
    public boolean isManager(List<String> adGroups, String userCase) {
        if (adGroups == null || adGroups.isEmpty() || userCase == null || userCase.isBlank()) {
            return false;
        }
        String expectedManagerGroup = adGroupPrefix + userCase + adGroupManagerSuffix;
        log.debug("Checking for manager group: {}", expectedManagerGroup);
        return adGroups.stream()
                .anyMatch(expectedManagerGroup::equalsIgnoreCase);
    }

    public boolean isUser(List<String> adGroups, String userCase) {
        if (adGroups == null || adGroups.isEmpty() || userCase == null || userCase.isBlank()) {
            return false;
        }
        String expectedUserGroup = adGroupPrefix + userCase + adGroupUserSuffix;
        log.debug("Checking for user group: {}", expectedUserGroup);
        return adGroups.stream()
                .anyMatch(expectedUserGroup::equalsIgnoreCase);
    }
}