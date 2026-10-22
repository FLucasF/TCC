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
        if (atCount != 1 || atIndex == 0 || atIndex == email.length() - 1) {
            return false;
        }
        String domain = email.substring(atIndex + 1);
        return domain.contains(".");
    }
}
