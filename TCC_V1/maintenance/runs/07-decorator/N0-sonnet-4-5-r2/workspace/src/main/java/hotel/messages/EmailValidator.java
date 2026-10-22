package hotel.messages;

public final class EmailValidator {

    public static boolean isValid(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex != email.lastIndexOf('@')) {
            return false;
        }
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex + 1);
        if (localPart.isBlank() || domainPart.isBlank()) {
            return false;
        }
        return domainPart.contains(".");
    }
}
