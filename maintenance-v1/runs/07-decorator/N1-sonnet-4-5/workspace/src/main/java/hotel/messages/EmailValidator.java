package hotel.messages;

/** Validates e-mail addresses according to the inn's needs. */
public final class EmailValidator {

    /**
     * An e-mail is valid when it has one @ character, something before it,
     * and a domain with at least one dot after it.
     */
    public static boolean isValid(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        long atCount = email.chars().filter(ch -> ch == '@').count();
        if (atCount != 1) {
            return false;
        }

        int atPosition = email.indexOf('@');
        if (atPosition == 0) {
            return false;
        }

        String domain = email.substring(atPosition + 1);
        if (domain.isEmpty() || !domain.contains(".")) {
            return false;
        }

        return true;
    }
}
