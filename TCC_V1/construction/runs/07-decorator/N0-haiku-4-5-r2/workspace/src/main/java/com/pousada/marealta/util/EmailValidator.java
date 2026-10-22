package com.pousada.marealta.util;

public class EmailValidator {
    public static boolean isValid(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        int atCount = 0;
        int atIndex = -1;
        for (int i = 0; i < email.length(); i++) {
            if (email.charAt(i) == '@') {
                atCount++;
                atIndex = i;
            }
        }

        if (atCount != 1) {
            return false;
        }

        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex + 1);

        if (local.isEmpty() || domain.isEmpty()) {
            return false;
        }

        int dotCount = 0;
        for (char c : domain.toCharArray()) {
            if (c == '.') {
                dotCount++;
            }
        }

        if (dotCount < 1) {
            return false;
        }

        return !domain.startsWith(".") && !domain.endsWith(".");
    }
}
