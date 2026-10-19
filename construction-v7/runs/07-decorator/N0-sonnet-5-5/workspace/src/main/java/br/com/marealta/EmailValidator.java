package br.com.marealta;

public final class EmailValidator {
    private EmailValidator() {}

    /** Um único @, algo antes dele e, depois, um nome com ponto no meio (sem rótulos vazios). */
    public static boolean isValid(String email) {
        if (email == null) return false;
        for (int i = 0; i < email.length(); i++) {
            if (Character.isWhitespace(email.charAt(i))) return false;
        }
        int at = email.indexOf('@');
        if (at <= 0 || at != email.lastIndexOf('@')) return false;
        String domain = email.substring(at + 1);
        if (!domain.contains(".")) return false;
        for (String label : domain.split("\\.", -1)) {
            if (label.isEmpty()) return false;
        }
        return true;
    }
}
