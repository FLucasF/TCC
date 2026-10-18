package hotel.messages;

import java.util.List;

/**
 * A rule that processes outgoing emails. It can modify the email, block it, or generate additional
 * emails (like archive copies).
 */
public interface EmailRule {
    /**
     * Processes an email and optionally generates additional emails.
     *
     * @param email the email being processed
     * @return additional emails to send (e.g., archive copies), or an empty list
     */
    List<OutgoingEmail> process(OutgoingEmail email);
}
