package com.dam.digitalassetmanagement.enums;

public enum UserRole {
    ADMIN("Administrator with full access"),
    UPLOADER("Can upload and manage own assets"),
    EDITOR("Can edit and approve assets"),
    VIEWER("Can only view approved assets");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}