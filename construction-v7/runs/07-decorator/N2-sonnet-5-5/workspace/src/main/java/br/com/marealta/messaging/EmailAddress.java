package br.com.marealta.messaging;

public final class EmailAddress {
    private EmailAddress() {}

    /** Um único @, algo antes dele e, depois, um nome com pelo menos um ponto no meio. */
    public static boolean isValid(String address) {
        if (address == null || address.chars().anyMatch(Character::isWhitespace)) return false;
        int at = address.indexOf('@');
        if (at <= 0 || at != address.lastIndexOf('@')) return false;
        String domain = address.substring(at + 1);
        if (!domain.contains(".")) return false;
        for (String label : domain.split("\\.", -1)) {
            if (label.isEmpty()) return false;
        }
        return true;
    }
}
