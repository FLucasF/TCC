package br.com.marealta;

import br.com.marealta.Config.Notifications;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.ToIntFunction;

/**
 * Regras das mensagens enviadas. Uma mensagem entra como rascunho, passa por uma cadeia de regras
 * e sai como uma ou mais mensagens (a original, uma possível cópia para o arquivo), enviadas ou bloqueadas.
 *
 * O redirecionamento do modo de teste é sempre a última etapa e age sobre todas as mensagens
 * que vão sair, para que nada chegue a um endereço real nesse modo.
 */
public final class Messaging {
    private Messaging() {}

    public enum Channel { EMAIL }

    public enum MessageStatus { SENT, BLOCKED }

    public record Draft(String to, String subject, String body) {}

    /**
     * @param guestAddress endereço original do hóspede (para contar o limite diário)
     * @param copy true se for a cópia para o arquivo
     */
    public record Outgoing(
            Channel channel, String to, String subject, String body, MessageStatus status,
            String guestAddress, boolean copy) {

        boolean sent() {
            return status == MessageStatus.SENT;
        }
    }

    /** O que as regras precisam saber do momento do envio. */
    public record Context(boolean testMode, ZonedDateTime now, ToIntFunction<String> sentTodayTo) {}

    public interface Rule {
        void apply(List<Outgoing> messages, Context ctx);
    }

    public static boolean isValidAddress(String a) {
        if (a == null || a.isEmpty() || a.chars().anyMatch(Character::isWhitespace)) {
            return false;
        }
        int at = a.indexOf('@');
        if (at <= 0 || at != a.lastIndexOf('@')) {
            return false;
        }
        String[] labels = a.substring(at + 1).split("\\.", -1);
        if (labels.length < 2) {
            return false;
        }
        for (String l : labels) {
            if (l.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static final class Pipeline {
        private final List<Rule> rules = new ArrayList<>();

        public Pipeline(Notifications n, boolean testMode) {
            if (n.addressValidation().appliesTo(testMode)) {
                rules.add(blockWhen((m, c) -> !isValidAddress(m.to())));
            }
            if (n.quietHours().appliesTo(testMode)) {
                rules.add(blockWhen((m, c) -> inQuietHours(c.now().toLocalTime(), n.quietStart(), n.quietEnd())));
            }
            if (n.dailyLimit().appliesTo(testMode)) {
                rules.add(blockWhen((m, c) -> c.sentTodayTo().applyAsInt(m.guestAddress()) >= n.maxPerGuestPerDay()));
            }
            if (n.archiveCopy().appliesTo(testMode)) {
                rules.add(archiveCopy(n.archiveAddress()));
            }
            if (testMode) {
                rules.add(testRedirect(n.testAddress()));
            }
        }

        public List<Outgoing> process(Draft draft, Context ctx) {
            List<Outgoing> out = new ArrayList<>();
            out.add(new Outgoing(Channel.EMAIL, draft.to(), draft.subject(), draft.body(),
                    MessageStatus.SENT, draft.to(), false));
            for (Rule r : rules) {
                r.apply(out, ctx);
            }
            return out;
        }
    }

    interface Condition {
        boolean test(Outgoing m, Context c);
    }

    static boolean inQuietHours(LocalTime t, LocalTime start, LocalTime end) {
        if (start.equals(end)) {
            return false;
        }
        return start.isBefore(end)
                ? !t.isBefore(start) && t.isBefore(end)
                : !t.isBefore(start) || t.isBefore(end);
    }

    /** Bloqueia a mensagem original; o endereço fica como estava. */
    private static Rule blockWhen(Condition cond) {
        return (messages, ctx) -> {
            for (int i = 0; i < messages.size(); i++) {
                Outgoing m = messages.get(i);
                if (m.sent() && !m.copy() && cond.test(m, ctx)) {
                    messages.set(i, new Outgoing(m.channel(), m.to(), m.subject(), m.body(),
                            MessageStatus.BLOCKED, m.guestAddress(), false));
                }
            }
        };
    }

    private static Rule archiveCopy(String archive) {
        return (messages, ctx) -> {
            for (int i = 0; i < messages.size(); i++) {
                Outgoing m = messages.get(i);
                if (m.sent() && !m.copy()) {
                    messages.add(i + 1, new Outgoing(m.channel(), archive, "[Cópia] " + m.subject(),
                            m.body(), MessageStatus.SENT, m.guestAddress(), true));
                    i++;
                }
            }
        };
    }

    private static Rule testRedirect(String testAddress) {
        return (messages, ctx) -> {
            for (int i = 0; i < messages.size(); i++) {
                Outgoing m = messages.get(i);
                if (m.sent()) {
                    messages.set(i, new Outgoing(m.channel(), testAddress, "[TESTE] " + m.subject(),
                            "Destinatário original: " + m.to() + "\n" + m.body(),
                            MessageStatus.SENT, m.guestAddress(), m.copy()));
                }
            }
        };
    }

    static String key(String address) {
        return address == null ? "" : address.toLowerCase(Locale.ROOT);
    }
}
