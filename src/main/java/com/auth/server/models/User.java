package com.auth.server.models;

public class User {
    private String username;
    private String passwordHash;
    private String role;
    private boolean locked;

    // Конструкторы
    public User() {}

    public User(String username, String passwordHash, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    // Геттеры
    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRole() {
        return role;
    }

    public boolean isLocked() {  // Особенность boolean геттеров
        return locked;
    }

    // Сеттеры
    public void setUsername(String username) {
        this.username = username;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }
}