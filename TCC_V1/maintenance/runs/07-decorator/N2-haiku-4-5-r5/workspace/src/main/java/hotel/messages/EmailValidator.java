package hotel.messages;

public final class EmailValidator {
    public static boolean isValid(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex != email.lastIndexOf('@')) {
            return false;
        }
        int dotIndex = email.indexOf('.', atIndex + 1);
        return dotIndex > atIndex + 1 && dotIndex < email.length() - 1;
    }

    private EmailValidator() {}
}
