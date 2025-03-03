package com.auth.server.utils;

import com.auth.server.dto.AuthRequest;

public class Validator {
    public static boolean isValidCredentials(AuthRequest request) {
        return isValidUsername(request.getUsername()) &&
                isValidPassword(request.getPassword());
    }

    private static boolean isValidUsername(String username) {
        return username != null && username.matches("^[a-zA-Z0-9]{5,20}$");
    }

    private static boolean isValidPassword(String password) {
        int minLength = Config.getInt("password.min.length");
        return password != null &&
                password.length() >= minLength &&
                password.matches("^(?=.*[A-Z])(?=.*\\d).+$");
    }
}