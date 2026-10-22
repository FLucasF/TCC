package hotel.messages;

public final class EmailValidator {

    public boolean isValid(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        int atCount = 0;
        int atPosition = -1;
        for (int i = 0; i < email.length(); i++) {
            if (email.charAt(i) == '@') {
                atCount++;
                atPosition = i;
            }
        }
        if (atCount != 1 || atPosition == 0 || atPosition == email.length() - 1) {
            return false;
        }
        String domain = email.substring(atPosition + 1);
        return domain.contains(".");
    }
}
