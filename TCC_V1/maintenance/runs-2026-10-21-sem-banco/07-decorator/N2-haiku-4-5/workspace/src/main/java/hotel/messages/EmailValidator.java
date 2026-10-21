package hotel.messages;

public final class EmailValidator {

    private EmailValidator() {}

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

        String before = email.substring(0, atIndex);
        String after = email.substring(atIndex + 1);

        if (before.isBlank() || after.isBlank()) {
            return false;
        }

        return after.contains(".");
    }
}
