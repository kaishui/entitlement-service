package com.kaishui.entitlement.constant;

/**
 * Defines the different types of resources that can be managed
 * for entitlement purposes.
 */
public enum ResouceType {
    /**
     * Represents a web page or a significant section within a web application.
     * Permissions might control visibility or access to the entire page.
     */
    PAGE,

    /**
     * Represents an Application Programming Interface endpoint or a specific URI pattern.
     * Permissions typically control whether a user/role can access this resource,
     * often associated with specific HTTP methods (GET, POST, etc.).
     */
    API,

    /**
     * Represents an interactive element within a user interface, like a button or link.
     * Permissions might control whether the element is visible, enabled, or clickable.
     */
    BUTTON
}