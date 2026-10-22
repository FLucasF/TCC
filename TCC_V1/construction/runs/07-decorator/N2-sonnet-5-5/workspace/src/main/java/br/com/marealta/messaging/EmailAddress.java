package br.com.marealta.messaging;

public final class EmailAddress {
    private EmailAddress() {}

    /** Um único @, algo antes dele e, depois, um nome com pelo menos um ponto no meio. */
    public static boolean isValid(String address) {
        if (address == null) return false;
        int at = address.indexOf('@');
        if (at < 1 || at != address.lastIndexOf('@')) return false;
        String domain = address.substring(at + 1);
        return hasInnerDot(domain);
    }

    private static boolean hasInnerDot(String domain) {
        for (int i = 1; i < domain.length() - 1; i++) {
            if (domain.charAt(i) == '.') return true;
        }
        return false;
    }
}
