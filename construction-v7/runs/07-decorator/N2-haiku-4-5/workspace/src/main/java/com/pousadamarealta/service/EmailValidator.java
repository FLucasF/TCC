package com.pousadamarealta.service;

public class EmailValidator {
    public static boolean isValid(String email) {
        if (email == null || email.isEmpty()) {
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

        String beforeAt = email.substring(0, atIndex);
        String afterAt = email.substring(atIndex + 1);

        if (beforeAt.isEmpty()) {
            return false;
        }

        if (afterAt.isEmpty()) {
            return false;
        }

        int dotCount = 0;
        for (int i = 0; i < afterAt.length(); i++) {
            if (afterAt.charAt(i) == '.') {
                dotCount++;
            }
        }

        if (dotCount < 1) {
            return false;
        }

        if (afterAt.startsWith(".") || afterAt.endsWith(".")) {
            return false;
        }

        return true;
    }
}
