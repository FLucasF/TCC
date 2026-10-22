package hotel.messages;

public final class EmailValidator {

    private EmailValidator() {}

    public static boolean isValid(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        int atIndex = email.indexOf('@');
        if (atIndex < 1 || email.lastIndexOf('@') != atIndex) {
            return false;
        }

        String domain = email.substring(atIndex + 1);
        if (domain.isEmpty() || domain.charAt(0) == '.' || domain.endsWith(".")) {
            return false;
        }

        return domain.contains(".");
    }
}
