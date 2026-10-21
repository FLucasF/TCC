package com.pousadamarealta.util;

import java.util.regex.Pattern;

public class EmailValidator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[^@]+@[^@]+\\.[^@]+$"
    );

    public static boolean isValid(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }

        int atCount = 0;
        for (char c : email.toCharArray()) {
            if (c == '@') atCount++;
        }
        if (atCount != 1) {
            return false;
        }

        String[] parts = email.split("@");
        if (parts.length != 2) {
            return false;
        }

        String before = parts[0];
        String after = parts[1];

        if (before.isEmpty()) {
            return false;
        }

        if (after.indexOf('.') <= 0 || after.indexOf('.') >= after.length() - 1) {
            return false;
        }

        return true;
    }
}
