package br.com.marealta.mail;

public final class EmailAddress {
    private EmailAddress() {
    }

    /** Um único @, algo antes dele e, depois, um nome com pelo menos um ponto no meio. */
    public static boolean isValid(String address) {
        if (address == null || address.chars().anyMatch(Character::isWhitespace)) {
            return false;
        }
        int at = address.indexOf('@');
        if (at <= 0 || at != address.lastIndexOf('@')) {
            return false;
        }
        String domain = address.substring(at + 1);
        int firstDot = domain.indexOf('.');
        return firstDot > 0 && !domain.endsWith(".");
    }
}
