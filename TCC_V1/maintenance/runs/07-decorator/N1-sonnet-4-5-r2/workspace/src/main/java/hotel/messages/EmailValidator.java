package hotel.messages;

final class EmailValidator {

    boolean isValid(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        int atPosition = email.indexOf('@');
        if (atPosition <= 0 || atPosition != email.lastIndexOf('@')) {
            return false;
        }
        String afterAt = email.substring(atPosition + 1);
        return !afterAt.isEmpty() && afterAt.contains(".");
    }
}
