package hotel.messages;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

/**
 * Sends e-mails to guests. Every message is kept in the list of sent messages, either SENT or BLOCKED. A message is
 * blocked when the address is invalid, in the quiet hours, or over the guest's daily limit (the last two only if the
 * installation asked for them). In test mode the message goes to the test address instead of the guest's. Outside
 * test mode, each message sent also leaves a copy for the archive.
 */
public final class EmailSender {

    private static final String CHANNEL = "EMAIL";

    private final SentMessages sentMessages;
    private final MailSettings settings;
    private final Clock clock;

    public EmailSender(SentMessages sentMessages) {
        this(sentMessages, MailSettings.defaults());
    }

    public EmailSender(SentMessages sentMessages, MailSettings settings) {
        this(sentMessages, settings, Clock.systemUTC());
    }

    public EmailSender(SentMessages sentMessages, MailSettings settings, Clock clock) {
        this.sentMessages = sentMessages;
        this.settings = settings;
        this.clock = clock;
    }

    public synchronized Message send(String to, String subject, String body) {
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(settings.zone()));
        if (!isValidAddress(to) || inQuietHours(now.toLocalTime()) || overDailyLimit(to, now)) {
            return sentMessages.add(CHANNEL, to, subject, body, MessageStatus.BLOCKED, null, now.toInstant());
        }
        if (settings.testMode()) {
            return sentMessages.add(
                    CHANNEL,
                    MailSettings.TEST_ADDRESS,
                    "[TESTE] " + subject,
                    "Destinatário original: " + to + "\n" + body,
                    MessageStatus.SENT,
                    to,
                    now.toInstant());
        }
        Message message = sentMessages.add(CHANNEL, to, subject, body, MessageStatus.SENT, null, now.toInstant());
        if (settings.archiveCopy()) {
            sentMessages.add(
                    CHANNEL,
                    MailSettings.ARCHIVE_ADDRESS,
                    "[Cópia] " + subject,
                    body,
                    MessageStatus.SENT,
                    null,
                    now.toInstant());
        }
        return message;
    }

    /** One single @, something before it, and after it a name with a dot in the middle. */
    static boolean isValidAddress(String address) {
        if (address == null) {
            return false;
        }
        int at = address.indexOf('@');
        if (at < 1 || at != address.lastIndexOf('@')) {
            return false;
        }
        String domain = address.substring(at + 1);
        int dot = domain.indexOf('.');
        return dot > 0 && domain.lastIndexOf('.') < domain.length() - 1 && dot < domain.length() - 1;
    }

    private boolean inQuietHours(LocalTime time) {
        if (settings.quietFrom() == null || !settings.quietScope().applies(settings.testMode())) {
            return false;
        }
        LocalTime from = settings.quietFrom();
        LocalTime until = settings.quietUntil();
        if (from.equals(until)) {
            return false;
        }
        boolean crossesMidnight = from.isAfter(until);
        return crossesMidnight
                ? !time.isBefore(from) || time.isBefore(until)
                : !time.isBefore(from) && time.isBefore(until);
    }

    private boolean overDailyLimit(String to, ZonedDateTime now) {
        if (settings.dailyLimit() == null || !settings.limitScope().applies(settings.testMode())) {
            return false;
        }
        LocalDate today = now.toLocalDate();
        int already = sentMessages.countSentToGuest(
                to,
                today.atStartOfDay(settings.zone()).toInstant(),
                today.plusDays(1).atStartOfDay(settings.zone()).toInstant());
        return already >= settings.dailyLimit();
    }
}
