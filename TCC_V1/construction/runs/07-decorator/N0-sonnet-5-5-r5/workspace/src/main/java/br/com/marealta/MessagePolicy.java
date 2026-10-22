package br.com.marealta;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Regras aplicadas a toda mensagem antes de sair. Cada regra opcional tem um escopo
 * (sempre, só no modo de teste, só fora dele, ou desligada), definido por instalação.
 *
 * Ordem: regras de bloqueio -> cópia para o arquivo -> redirecionamento do modo de teste.
 * O redirecionamento vale para tudo o que sai (inclusive a cópia), então nada chega a endereço real
 * em modo de teste, qualquer que seja a combinação de regras configurada.
 */
public final class MessagePolicy {

    public enum Scope {
        OFF, ALWAYS, TEST_ONLY, LIVE_ONLY;

        boolean applies(boolean testMode) {
            return switch (this) {
                case OFF -> false;
                case ALWAYS -> true;
                case TEST_ONLY -> testMode;
                case LIVE_ONLY -> !testMode;
            };
        }
    }

    public record Config(
            boolean testMode,
            String testAddress,
            String archiveAddress,
            Scope emailValidation,
            Scope archiveCopy,
            Scope quietHours,
            LocalTime quietStart,
            LocalTime quietEnd,
            Scope dailyLimit,
            int dailyLimitMax) {}

    public record Draft(String to, String subject, String body) {}

    /** Mensagem pronta: {@code originalTo} é o destinatário que seria o certo (para limite diário e auditoria). */
    public record Out(String to, String subject, String body, boolean blocked, String originalTo, boolean copy) {}

    private final Config cfg;

    public MessagePolicy(Config cfg) {
        this.cfg = cfg;
    }

    public static boolean isValidEmail(String a) {
        if (a == null) return false;
        int at = a.indexOf('@');
        if (at <= 0 || at != a.lastIndexOf('@')) return false;
        String domain = a.substring(at + 1);
        return domain.contains(".") && !domain.startsWith(".") && !domain.endsWith(".");
    }

    /** @param sentToday quantas mensagens (não cópias) já foram enviadas hoje a esse endereço original */
    public List<Out> process(Draft d, LocalTime now, ToIntFunction<String> sentToday) {
        if (blocked(d, now, sentToday)) {
            return List.of(new Out(d.to(), d.subject(), d.body(), true, d.to(), false));
        }
        List<Draft> drafts = new ArrayList<>();
        List<Boolean> copies = new ArrayList<>();
        drafts.add(d);
        copies.add(false);
        if (cfg.archiveCopy().applies(cfg.testMode())) {
            drafts.add(new Draft(cfg.archiveAddress(), "[Cópia] " + d.subject(), d.body()));
            copies.add(true);
        }
        List<Out> out = new ArrayList<>();
        for (int i = 0; i < drafts.size(); i++) {
            Draft x = drafts.get(i);
            if (cfg.testMode()) {
                out.add(new Out(cfg.testAddress(), "[TESTE] " + x.subject(),
                        "Destinatário original: " + x.to() + "\n" + x.body(), false, x.to(), copies.get(i)));
            } else {
                out.add(new Out(x.to(), x.subject(), x.body(), false, x.to(), copies.get(i)));
            }
        }
        return out;
    }

    private boolean blocked(Draft d, LocalTime now, ToIntFunction<String> sentToday) {
        if (cfg.emailValidation().applies(cfg.testMode()) && !isValidEmail(d.to())) return true;
        if (cfg.quietHours().applies(cfg.testMode()) && inQuietHours(now)) return true;
        return cfg.dailyLimit().applies(cfg.testMode()) && sentToday.applyAsInt(d.to()) >= cfg.dailyLimitMax();
    }

    private boolean inQuietHours(LocalTime now) {
        LocalTime s = cfg.quietStart(), e = cfg.quietEnd();
        if (s.equals(e)) return false;
        return s.isBefore(e) ? !now.isBefore(s) && now.isBefore(e) : !now.isBefore(s) || now.isBefore(e);
    }
}
