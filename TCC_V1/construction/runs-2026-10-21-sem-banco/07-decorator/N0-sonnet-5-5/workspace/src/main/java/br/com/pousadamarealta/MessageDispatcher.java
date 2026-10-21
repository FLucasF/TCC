package br.com.pousadamarealta;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs every outgoing message through an ordered chain of rules, then records the result in the
 * {@link MessageLog}. Each rule has a {@link Scope} deciding whether it runs in the current mode.
 */
public class MessageDispatcher {

    /** A message being processed; rules may block it, rewrite it or add copies. */
    static final class Draft {
        final String originalTo;
        final String originalSubject;
        final String originalBody;
        String to;
        String subject;
        String body;
        boolean blocked;
        final List<Draft> copies = new ArrayList<>();

        Draft(String to, String subject, String body) {
            this.originalTo = to;
            this.originalSubject = subject;
            this.originalBody = body;
            this.to = to;
            this.subject = subject;
            this.body = body;
        }
    }

    interface Rule {
        void apply(Draft draft);
    }

    private record Step(Scope scope, Rule rule) {}

    private final MessageLog log;
    private final boolean testMode;
    private final List<Step> steps = new ArrayList<>();

    public MessageDispatcher(AppConfig config, Clock clock, MessageLog log) {
        this.log = log;
        this.testMode = config.testMode();

        steps.add(new Step(Scope.ALWAYS, d -> d.blocked = !validAddress(d.to)));
        steps.add(new Step(config.quietHours(), new QuietHours(clock, config)));
        steps.add(new Step(config.dailyLimit(), new DailyLimit(clock, config)));
        steps.add(new Step(Scope.TEST_ONLY, MessageDispatcher::redirectToTest));
        steps.add(new Step(config.archiveCopy(), MessageDispatcher::addArchiveCopy));
    }

    public synchronized void sendEmail(String to, String subject, String body) {
        Draft draft = new Draft(to, subject, body);
        for (Step step : steps) {
            if (!draft.blocked && step.scope().appliesTo(testMode)) {
                step.rule().apply(draft);
            }
        }
        if (draft.blocked) {
            log.add(Message.Channel.EMAIL, draft.originalTo, draft.originalSubject, draft.originalBody,
                    Message.Status.BLOCKED);
            return;
        }
        log.add(Message.Channel.EMAIL, draft.to, draft.subject, draft.body, Message.Status.SENT);
        for (Draft copy : draft.copies) {
            log.add(Message.Channel.EMAIL, copy.to, copy.subject, copy.body, Message.Status.SENT);
        }
    }

    /** Exactly one '@', something before it, and a domain with a dot that is not at either end. */
    static boolean validAddress(String address) {
        if (address == null || address.chars().anyMatch(Character::isWhitespace)) return false;
        int at = address.indexOf('@');
        if (at < 1 || at != address.lastIndexOf('@')) return false;
        String domain = address.substring(at + 1);
        return domain.indexOf('.') > 0 && !domain.endsWith(".");
    }

    private static void redirectToTest(Draft d) {
        d.body = "Destinatário original: " + d.to + "\n" + d.body;
        d.subject = "[TESTE] " + d.subject;
        d.to = AppConfig.TEST_ADDRESS;
    }

    private static void addArchiveCopy(Draft d) {
        d.copies.add(new Draft(AppConfig.ARCHIVE_ADDRESS, "[Cópia] " + d.subject, d.body));
    }

    private static final class QuietHours implements Rule {
        private final Clock clock;
        private final AppConfig config;

        QuietHours(Clock clock, AppConfig config) {
            this.clock = clock;
            this.config = config;
        }

        @Override
        public void apply(Draft d) {
            int hour = ZonedDateTime.now(clock.withZone(config.zone())).getHour();
            int start = config.quietStartHour();
            int end = config.quietEndHour();
            d.blocked = start <= end ? hour >= start && hour < end : hour >= start || hour < end;
        }
    }

    /** At most N sent messages per guest address per calendar day. */
    private static final class DailyLimit implements Rule {
        private final Clock clock;
        private final AppConfig config;
        private final Map<String, Integer> counts = new HashMap<>();

        DailyLimit(Clock clock, AppConfig config) {
            this.clock = clock;
            this.config = config;
        }

        @Override
        public void apply(Draft d) {
            LocalDate today = LocalDate.now(clock.withZone(config.zone()));
            counts.keySet().removeIf(k -> !k.startsWith(today + "|"));
            String key = today + "|" + d.originalTo.toLowerCase();
            int used = counts.getOrDefault(key, 0);
            if (used >= config.maxMessagesPerGuestPerDay()) {
                d.blocked = true;
            } else {
                counts.put(key, used + 1);
            }
        }
    }
}
