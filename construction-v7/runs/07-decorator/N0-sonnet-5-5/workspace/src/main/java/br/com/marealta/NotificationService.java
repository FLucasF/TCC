package br.com.marealta;

import java.util.ArrayList;
import java.util.List;

/**
 * Envio de e-mails. Por enquanto nada sai de verdade: as mensagens ficam numa lista.
 * Cada regra declara em que modo vale (sempre, só teste ou só fora do teste) e roda em ordem.
 * Novas regras (horário, limite por hóspede) entram como mais um {@link Rule} na lista.
 */
public class NotificationService {
    public static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";
    public static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";

    public enum Scope { ALWAYS, TEST_ONLY, NON_TEST_ONLY }

    /** Mensagem em construção, que as regras podem alterar, bloquear ou acompanhar de cópias. */
    public static class Draft {
        public String to;
        public String subject;
        public String body;
        public boolean blocked;
        public final List<Draft> extras = new ArrayList<>();

        Draft(String to, String subject, String body) {
            this.to = to;
            this.subject = subject;
            this.body = body;
        }
    }

    public interface Rule {
        Scope scope();
        void apply(Draft draft);
    }

    private final boolean testMode;
    private final List<Rule> rules;
    private final List<Message> sent = new ArrayList<>();

    public NotificationService(boolean testMode) {
        this.testMode = testMode;
        this.rules = List.of(
                // 1. endereço inválido: bloqueia (com o endereço original)
                new Rule() {
                    public Scope scope() { return Scope.ALWAYS; }
                    public void apply(Draft d) {
                        if (!EmailValidator.isValid(d.to)) d.blocked = true;
                    }
                },
                // 2. modo de teste: nunca sai para o endereço real
                new Rule() {
                    public Scope scope() { return Scope.TEST_ONLY; }
                    public void apply(Draft d) {
                        if (d.blocked) return;
                        d.body = "Destinatário original: " + d.to + "\n" + d.body;
                        d.subject = "[TESTE] " + d.subject;
                        d.to = TEST_ADDRESS;
                    }
                },
                // 3. cópia para o arquivo do contador
                new Rule() {
                    public Scope scope() { return Scope.NON_TEST_ONLY; }
                    public void apply(Draft d) {
                        if (d.blocked) return;
                        d.extras.add(new Draft(ARCHIVE_ADDRESS, "[Cópia] " + d.subject, d.body));
                    }
                });
    }

    public boolean isTestMode() { return testMode; }

    public synchronized void sendEmail(String to, String subject, String body) {
        Draft draft = new Draft(to, subject, body);
        for (Rule r : rules) {
            boolean applies = switch (r.scope()) {
                case ALWAYS -> true;
                case TEST_ONLY -> testMode;
                case NON_TEST_ONLY -> !testMode;
            };
            if (applies) r.apply(draft);
        }
        record(draft);
        for (Draft extra : draft.extras) record(extra);
    }

    private void record(Draft d) {
        sent.add(new Message(sent.size() + 1, "EMAIL", d.to, d.subject, d.body, d.blocked ? "BLOCKED" : "SENT"));
    }

    public synchronized List<Message> messages() {
        return List.copyOf(sent);
    }
}
