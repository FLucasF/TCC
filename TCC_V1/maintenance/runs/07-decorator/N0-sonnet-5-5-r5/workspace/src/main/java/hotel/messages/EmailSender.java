package hotel.messages;

import java.time.Clock;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

/**
 * Sends e-mails to guests, following the installation's {@link MessagingConfig}. Every message is kept in the list of
 * sent messages: either SENT, or BLOCKED when a rule stopped it (nothing goes out, the front desk calls the guest).
 */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final MessagingConfig config;
    private final Clock clock;

    public EmailSender(SentMessages sentMessages) {
        this(sentMessages, MessagingConfig.defaults(), Clock.systemUTC());
    }

    public EmailSender(SentMessages sentMessages, MessagingConfig config, Clock clock) {
        this.sentMessages = sentMessages;
        this.config = config;
        this.clock = clock;
    }

    public synchronized Message send(String to, String subject, String body) {
        boolean test = config.testMode();
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(config.zone()));
        OffsetDateTime at = now.toOffsetDateTime();

        if (config.addressCheck().applies(test) && !isValidAddress(to)) {
            return sentMessages.add("EMAIL", to, to, subject, body, MessageStatus.BLOCKED, false, at);
        }
        if (config.quietHours().applies(test) && isQuiet(now.toLocalTime())) {
            return sentMessages.add("EMAIL", to, to, subject, body, MessageStatus.BLOCKED, false, at);
        }
        if (config.dailyLimit().applies(test)
                && sentMessages.countSentToGuestOn(to, now.toLocalDate(), config.zone()) >= config.maxPerGuestPerDay()) {
            return sentMessages.add("EMAIL", to, to, subject, body, MessageStatus.BLOCKED, false, at);
        }

        String actualTo = to;
        String actualSubject = subject;
        String actualBody = body;
        if (test) {
            actualTo = MessagingConfig.TEST_ADDRESS;
            actualSubject = "[TESTE] " + subject;
            actualBody = "Destinatário original: " + to + "\n" + body;
        }
        Message sent = sentMessages.add("EMAIL", actualTo, to, actualSubject, actualBody, MessageStatus.SENT, false, at);
        if (config.archiveCopy().applies(test)) {
            sentMessages.add("EMAIL", MessagingConfig.ARCHIVE_ADDRESS, to, "[Cópia] " + actualSubject, actualBody,
                    MessageStatus.SENT, true, at);
        }
        return sent;
    }

    /** One single @, something before it, and after it a name with a dot in the middle. */
    static boolean isValidAddress(String address) {
        if (address == null) {
            return false;
        }
        int at = address.indexOf('@');
        if (at <= 0 || at != address.lastIndexOf('@')) {
            return false;
        }
        String domain = address.substring(at + 1);
        for (int i = 1; i < domain.length() - 1; i++) {
            if (domain.charAt(i) == '.') {
                return true;
            }
        }
        return false;
    }

    private boolean isQuiet(LocalTime time) {
        LocalTime from = config.quietFrom();
        LocalTime to = config.quietTo();
        if (from.equals(to)) {
            return false;
        }
        return from.isBefore(to)
                ? !time.isBefore(from) && time.isBefore(to)
                : !time.isBefore(from) || time.isBefore(to);
    }
}
