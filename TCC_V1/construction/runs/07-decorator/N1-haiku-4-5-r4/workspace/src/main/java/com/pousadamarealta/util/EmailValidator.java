package com.pousadamarealta.util;

import java.util.regex.Pattern;

public class EmailValidator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@]+@[^@]+\\.[^@]+$");

    public static boolean isValid(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || email.indexOf('@', atIndex + 1) != -1) {
            return false;
        }

        String afterAt = email.substring(atIndex + 1);
        return afterAt.contains(".") && !afterAt.startsWith(".") && !afterAt.endsWith(".");
    }
}
