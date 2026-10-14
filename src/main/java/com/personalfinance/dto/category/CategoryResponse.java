package com.personalfinance.dto.category;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CategoryResponse {
    private String name;
    private String type;
    private boolean isCustom;

    public CategoryResponse() {
    }

    public CategoryResponse(String name, String type, boolean isCustom) {
        this.name = name;
        this.type = type;
        this.isCustom = isCustom;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty("isCustom")
    public boolean getIsCustom() {
        return isCustom;
    }

    @JsonProperty("custom")
    public boolean getCustom() {
        return isCustom;
    }

    public void setIsCustom(boolean custom) {
        this.isCustom = custom;
    }
}
