package pousada.messaging;

public final class EmailAddress {
    private EmailAddress() {
    }

    /** Um único @, algo antes dele e, depois, um nome com ao menos um ponto no meio. */
    public static boolean isValid(String address) {
        if (address == null) {
            return false;
        }
        int at = address.indexOf('@');
        if (at <= 0 || at != address.lastIndexOf('@')) {
            return false;
        }
        String domain = address.substring(at + 1);
        int dot = domain.indexOf('.');
        while (dot >= 0) {
            if (dot > 0 && dot < domain.length() - 1) {
                return true;
            }
            dot = domain.indexOf('.', dot + 1);
        }
        return false;
    }
}
