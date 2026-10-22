package br.com.marealta.mail;

/** Em que modo uma regra de mensagem vale. */
public enum Scope {
    ALWAYS, TEST_ONLY, LIVE_ONLY, OFF;

    public boolean applies(boolean testMode) {
        return switch (this) {
            case ALWAYS -> true;
            case TEST_ONLY -> testMode;
            case LIVE_ONLY -> !testMode;
            case OFF -> false;
        };
    }

    static Scope parse(String raw, Scope fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        return valueOf(raw.trim().toUpperCase());
    }
}
