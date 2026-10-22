package com.pousada.utils;

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

        if (atIndex == 0 || atIndex == email.length() - 1) {
            return false;
        }

        String domain = email.substring(atIndex + 1);
        int dotCount = 0;
        for (char c : domain.toCharArray()) {
            if (c == '.') dotCount++;
        }

        if (dotCount == 0) {
            return false;
        }

        if (domain.startsWith(".") || domain.endsWith(".")) {
            return false;
        }

        return true;
    }
}
