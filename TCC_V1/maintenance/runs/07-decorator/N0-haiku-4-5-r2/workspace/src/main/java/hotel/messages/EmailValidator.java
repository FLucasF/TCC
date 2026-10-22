package hotel.messages;

public final class EmailValidator {

    private static final String EMAIL_REGEX = "^[^@]+@[^@]+\\.[^@]+$";

    public static boolean isValid(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return email.matches(EMAIL_REGEX);
    }
}
