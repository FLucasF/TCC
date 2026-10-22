package com.pousada.marealta.util;

public class EmailValidator {
    public static boolean isValid(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }

        int atIndex = email.indexOf('@');
        int lastAtIndex = email.lastIndexOf('@');

        if (atIndex == -1 || atIndex != lastAtIndex) {
            return false;
        }

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex + 1);

        if (localPart.isEmpty()) {
            return false;
        }

        int dotIndex = domain.indexOf('.');
        if (dotIndex <= 0 || dotIndex == domain.length() - 1) {
            return false;
        }

        return true;
    }
}
