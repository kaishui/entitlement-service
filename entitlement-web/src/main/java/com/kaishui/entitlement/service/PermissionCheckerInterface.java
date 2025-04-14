package com.kaishui.entitlement.service;

import reactor.core.publisher.Mono;

public interface PermissionCheckerInterface {
    Mono<Boolean> checkPermission(String staffId, String httpMethod, String requestUri);
}
