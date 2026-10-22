package br.com.marealta.pousada;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * Configuração da instalação, lida das variáveis de ambiente.
 *
 * <pre>
 * HOTEL_TEST_MODE=true                  modo de teste
 * HOTEL_ARCHIVE_COPY=NON_TEST_ONLY      cópia para o arquivo (ALWAYS | TEST_ONLY | NON_TEST_ONLY | NEVER)
 * HOTEL_QUIET_HOURS=22:00-07:00         não envia mensagens nesse intervalo (desligado se ausente)
 * HOTEL_QUIET_HOURS_SCOPE=ALWAYS        onde a regra vale
 * HOTEL_DAILY_LIMIT=5                   máximo de mensagens por hóspede por dia (desligado se ausente)
 * HOTEL_DAILY_LIMIT_SCOPE=ALWAYS        onde a regra vale
 * HOTEL_TIMEZONE=America/Sao_Paulo      fuso usado por "madrugada" e "por dia"
 * </pre>
 */
public record Config(boolean testMode, Scope archiveCopy, QuietHours quietHours, Scope quietHoursScope,
                     Integer dailyLimit, Scope dailyLimitScope, ZoneId zone) {

    /** Onde uma regra vale. */
    public enum Scope {
        ALWAYS, TEST_ONLY, NON_TEST_ONLY, NEVER;

        public boolean appliesIn(boolean testMode) {
            return switch (this) {
                case ALWAYS -> true;
                case TEST_ONLY -> testMode;
                case NON_TEST_ONLY -> !testMode;
                case NEVER -> false;
            };
        }
    }

    /** Intervalo [start, end) que pode atravessar a meia-noite. */
    public record QuietHours(LocalTime start, LocalTime end) {
        public boolean contains(LocalTime t) {
            if (start.equals(end)) return false;
            return start.isBefore(end)
                    ? !t.isBefore(start) && t.isBefore(end)
                    : !t.isBefore(start) || t.isBefore(end);
        }

        static QuietHours parse(String s) {
            String[] p = s.trim().split("-");
            if (p.length != 2) throw new IllegalArgumentException("HOTEL_QUIET_HOURS inválido: " + s);
            return new QuietHours(LocalTime.parse(p[0].trim()), LocalTime.parse(p[1].trim()));
        }
    }

    public static Config defaults(boolean testMode) {
        return fromEnv(Map.of("HOTEL_TEST_MODE", String.valueOf(testMode)));
    }

    public static Config fromEnv(Map<String, String> env) {
        boolean test = "true".equalsIgnoreCase(blankToNull(env.get("HOTEL_TEST_MODE")));
        String qh = blankToNull(env.get("HOTEL_QUIET_HOURS"));
        String dl = blankToNull(env.get("HOTEL_DAILY_LIMIT"));
        String tz = blankToNull(env.get("HOTEL_TIMEZONE"));
        return new Config(
                test,
                scope(env.get("HOTEL_ARCHIVE_COPY"), Scope.NON_TEST_ONLY),
                qh == null ? null : QuietHours.parse(qh),
                scope(env.get("HOTEL_QUIET_HOURS_SCOPE"), Scope.ALWAYS),
                dl == null ? null : Integer.valueOf(dl.trim()),
                scope(env.get("HOTEL_DAILY_LIMIT_SCOPE"), Scope.ALWAYS),
                ZoneId.of(tz == null ? "America/Sao_Paulo" : tz));
    }

    private static Scope scope(String v, Scope def) {
        v = blankToNull(v);
        return v == null ? def : Scope.valueOf(v.trim().toUpperCase());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
