package br.com.marealta.pousada;

import br.com.marealta.pousada.MailConfig.Scope;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Aplica as regras de envio e grava o resultado na lista de mensagens (e-mails ainda não saem de verdade). */
public class MailDispatcher {
    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    /** Mensagem a gravar. {@code originalTo} é o destinatário pretendido; {@code copy} marca cópias de arquivo. */
    public record NewMessage(String to, String subject, String body, String status,
                             String originalTo, boolean copy, Instant createdAt) {}

    public interface MailStore {
        void save(NewMessage m);

        /** Mensagens ENVIADAS (não cópias) cujo destinatário pretendido é {@code address}, no intervalo. */
        int countSent(String address, Instant fromInclusive, Instant toExclusive);
    }

    private final MailConfig config;
    private final Clock clock;

    public MailDispatcher(MailConfig config, Clock clock) {
        this.config = config;
        this.clock = clock;
    }

    public static boolean validAddress(String a) {
        if (a == null) return false;
        int at = a.indexOf('@');
        if (at <= 0 || at != a.lastIndexOf('@')) return false;
        for (int i = 0; i < a.length(); i++) if (Character.isWhitespace(a.charAt(i))) return false;
        String domain = a.substring(at + 1);
        for (int i = 1; i < domain.length() - 1; i++) if (domain.charAt(i) == '.') return true;
        return false;
    }

    public void send(MailStore store, String to, String subject, String body) {
        Instant now = clock.instant();
        if (blocked(store, to, now)) {
            store.save(new NewMessage(to, subject, body, "BLOCKED", to, false, now));
            return;
        }
        List<NewMessage> out = new ArrayList<>();
        out.add(new NewMessage(to, subject, body, "SENT", to, false, now));
        if (config.archiveCopy().active(config.testMode())) {
            out.add(new NewMessage(MailConfig.ARCHIVE_ADDRESS, "[Cópia] " + subject, body, "SENT", to, true, now));
        }
        for (NewMessage m : out) store.save(config.testMode() ? redirected(m) : m);
    }

    /** No modo de teste nada sai para o endereço real, nem as cópias de arquivo. */
    private static NewMessage redirected(NewMessage m) {
        return new NewMessage(MailConfig.TEST_ADDRESS, "[TESTE] " + m.subject(),
                "Destinatário original: " + m.to() + "\n" + m.body(),
                m.status(), m.originalTo(), m.copy(), m.createdAt());
    }

    private boolean blocked(MailStore store, String to, Instant now) {
        if (!validAddress(to)) return true;
        ZonedDateTime local = now.atZone(ZONE);
        if (config.quietHours().active(config.testMode()) && inQuietHours(local.getHour())) return true;
        if (config.dailyLimit().active(config.testMode())) {
            LocalDate day = local.toLocalDate();
            Instant from = day.atStartOfDay(ZONE).toInstant();
            Instant until = day.plusDays(1).atStartOfDay(ZONE).toInstant();
            return store.countSent(to.toLowerCase(Locale.ROOT), from, until) >= config.dailyLimitMax();
        }
        return false;
    }

    private boolean inQuietHours(int hour) {
        int s = config.quietStartHour(), e = config.quietEndHour();
        if (s == e) return false;
        return s < e ? hour >= s && hour < e : hour >= s || hour < e;
    }
}
