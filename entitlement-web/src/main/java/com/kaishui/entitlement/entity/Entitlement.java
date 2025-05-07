package com.kaishui.entitlement.entity;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class Entitlement {
    private String adGroup;

    private List<String> roleIds;
}
