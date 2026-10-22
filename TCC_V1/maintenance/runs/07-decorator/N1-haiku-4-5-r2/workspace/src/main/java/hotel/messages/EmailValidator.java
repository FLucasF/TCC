package hotel.messages;

public final class EmailValidator {

    public static boolean isValid(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        int atIndex = email.indexOf('@');
        if (atIndex == -1 || atIndex == 0) {
            return false;
        }

        if (email.lastIndexOf('@') != atIndex) {
            return false;
        }

        String domain = email.substring(atIndex + 1);
        return domain.length() > 0 && domain.indexOf('.') != -1;
    }
}
