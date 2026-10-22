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
        if (atCount != 1) {
            return false;
        }
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex + 1);
        if (localPart.isEmpty() || domainPart.isEmpty()) {
            return false;
        }
        if (domainPart.startsWith(".") || domainPart.endsWith(".")) {
            return false;
        }
        int dotIndex = domainPart.indexOf('.');
        return dotIndex > 0;
    }
}
