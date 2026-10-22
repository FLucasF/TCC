package br.com.marealta.mail;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Decide o que realmente é guardado/enviado para uma mensagem desejada:
 * 1) regras de bloqueio (endereço inválido, madrugada, limite diário);
 * 2) redirecionamento do modo de teste;
 * 3) cópia para o arquivo.
 */
public final class MessagePolicy {
    private final MailSettings s;

    public MessagePolicy(MailSettings settings) {
        this.s = settings;
    }

    /** @param sentToday quantas mensagens já foram enviadas hoje ao endereço original dado */
    public List<Outgoing> apply(String to, String subject, String body, Instant now, ToIntFunction<String> sentToday) {
        List<Outgoing> out = new ArrayList<>();
        if (isBlocked(to, now, sentToday)) {
            out.add(new Outgoing(to, subject, body, Outgoing.BLOCKED, to));
            return out;
        }
        String finalTo = to, finalSubject = subject, finalBody = body;
        if (s.testMode()) {
            finalTo = s.testAddress();
            finalSubject = "[TESTE] " + subject;
            finalBody = "Destinatário original: " + to + "\n" + body;
        }
        out.add(new Outgoing(finalTo, finalSubject, finalBody, Outgoing.SENT, to));
        if (s.archiveCopy().applies(s.testMode())) {
            out.add(new Outgoing(s.archiveAddress(), "[Cópia] " + finalSubject, finalBody, Outgoing.SENT, null));
        }
        return out;
    }

    private boolean isBlocked(String to, Instant now, ToIntFunction<String> sentToday) {
        boolean test = s.testMode();
        if (s.addressValidation().applies(test) && !isValidAddress(to)) return true;
        if (s.quietHours().applies(test) && inQuietHours(now)) return true;
        return s.dailyLimit().applies(test) && sentToday.applyAsInt(to) >= s.dailyLimitMax();
    }

    private boolean inQuietHours(Instant now) {
        int h = now.atZone(s.zone()).getHour();
        int a = s.quietStartHour(), b = s.quietEndHour();
        return a <= b ? (h >= a && h < b) : (h >= a || h < b);
    }

    /** Um único @, algo antes dele e, depois, um nome com pelo menos um ponto no meio. */
    public static boolean isValidAddress(String address) {
        if (address == null || address.chars().anyMatch(Character::isWhitespace)) return false;
        int at = address.indexOf('@');
        if (at <= 0 || at != address.lastIndexOf('@')) return false;
        String domain = address.substring(at + 1);
        for (int i = 1; i < domain.length() - 1; i++) {
            if (domain.charAt(i) == '.') return true;
        }
        return false;
    }
}
