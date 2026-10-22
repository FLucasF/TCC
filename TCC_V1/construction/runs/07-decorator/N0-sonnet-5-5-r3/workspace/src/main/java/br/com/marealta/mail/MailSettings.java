package br.com.marealta.mail;

import java.time.ZoneId;
import java.util.Map;

/**
 * Combinação de regras de mensagem de uma instalação. Cada regra tem um {@link Scope}.
 * O redirecionamento do modo de teste não é configurável: sempre vale em modo de teste.
 */
public record MailSettings(
        boolean testMode,
        Scope addressValidation,
        Scope archiveCopy,
        Scope quietHours,
        Scope dailyLimit,
        int quietStartHour,
        int quietEndHour,
        int dailyLimitMax,
        String archiveAddress,
        String testAddress,
        ZoneId zone) {

    public static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";
    public static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";

    /** Padrão da pousada: validação sempre, cópia para o arquivo fora do teste, sem horário nem limite. */
    public static MailSettings defaults(boolean testMode) {
        return new MailSettings(testMode, Scope.ALWAYS, Scope.LIVE_ONLY, Scope.OFF, Scope.OFF,
                22, 7, 5, ARCHIVE_ADDRESS, TEST_ADDRESS, ZoneId.of("America/Sao_Paulo"));
    }

    public static MailSettings fromEnv(Map<String, String> env) {
        boolean test = "true".equalsIgnoreCase(env.getOrDefault("HOTEL_TEST_MODE", "").trim());
        MailSettings d = defaults(test);
        return new MailSettings(
                test,
                Scope.parse(env.get("HOTEL_MAIL_VALIDATION"), d.addressValidation),
                Scope.parse(env.get("HOTEL_MAIL_ARCHIVE_COPY"), d.archiveCopy),
                Scope.parse(env.get("HOTEL_MAIL_QUIET_HOURS"), d.quietHours),
                Scope.parse(env.get("HOTEL_MAIL_DAILY_LIMIT"), d.dailyLimit),
                intOf(env.get("HOTEL_MAIL_QUIET_START"), d.quietStartHour),
                intOf(env.get("HOTEL_MAIL_QUIET_END"), d.quietEndHour),
                intOf(env.get("HOTEL_MAIL_DAILY_LIMIT_MAX"), d.dailyLimitMax),
                env.getOrDefault("HOTEL_MAIL_ARCHIVE_ADDRESS", d.archiveAddress),
                env.getOrDefault("HOTEL_MAIL_TEST_ADDRESS", d.testAddress),
                env.get("HOTEL_ZONE") == null ? d.zone : ZoneId.of(env.get("HOTEL_ZONE")));
    }

    private static int intOf(String raw, int fallback) {
        return raw == null || raw.isBlank() ? fallback : Integer.parseInt(raw.trim());
    }
}
