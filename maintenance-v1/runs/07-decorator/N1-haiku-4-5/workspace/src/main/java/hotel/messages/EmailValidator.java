package hotel.messages;

public final class EmailValidator {

    public static boolean isValid(String email) {
        if (email == null || email.isBlank()) {
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

        if (beforeAt.isEmpty() || afterAt.isEmpty()) {
            return false;
        }

        int dotCount = 0;
        for (int i = 0; i < afterAt.length(); i++) {
            if (afterAt.charAt(i) == '.') {
                dotCount++;
            }
        }

        return dotCount >= 1;
    }
}
