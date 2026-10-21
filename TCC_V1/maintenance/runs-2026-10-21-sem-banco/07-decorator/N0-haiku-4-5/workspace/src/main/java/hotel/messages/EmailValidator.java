package hotel.messages;

public final class EmailValidator {

    public static boolean isValid(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        int atIndex = email.indexOf('@');
        if (atIndex < 1 || email.lastIndexOf('@') != atIndex) {
            return false;
        }
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex + 1);
        return !domain.isBlank() && domain.contains(".");
    }
}
