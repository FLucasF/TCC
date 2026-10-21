package hotel.messages;

public final class EmailValidator {
    public static boolean isValid(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        int atCount = 0;
        int lastAtIndex = -1;
        for (int i = 0; i < email.length(); i++) {
            if (email.charAt(i) == '@') {
                atCount++;
                lastAtIndex = i;
            }
        }
        if (atCount != 1 || lastAtIndex <= 0 || lastAtIndex == email.length() - 1) {
            return false;
        }
        String domain = email.substring(lastAtIndex + 1);
        int dotIndex = domain.indexOf('.');
        return dotIndex > 0 && dotIndex < domain.length() - 1;
    }
}
