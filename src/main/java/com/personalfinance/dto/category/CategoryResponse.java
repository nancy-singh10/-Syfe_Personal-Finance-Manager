package com.personalfinance.dto.category;

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

    public boolean isIsCustom() {
        return isCustom;
    }

    public void setIsCustom(boolean custom) {
        isCustom = custom;
    }

    // Standard getter for Jackson boolean serialization
    public boolean getIsCustom() {
        return isCustom;
    }
}
