package com.pousadamarealta.util;

public class EmailValidator {
    public static boolean isValid(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || email.indexOf('@', atIndex + 1) != -1) {
            return false;
        }

        String domain = email.substring(atIndex + 1);
        return domain.contains(".");
    }
}
