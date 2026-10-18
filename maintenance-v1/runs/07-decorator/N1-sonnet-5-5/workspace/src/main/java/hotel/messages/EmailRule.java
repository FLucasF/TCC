package hotel.messages;

import java.util.List;

/**
 * One rule applied to every e-mail before it goes out. It returns what should be recorded in place of the
 * e-mail: the e-mail changed, blocked, or followed by extra e-mails. Blocked e-mails never reach a rule.
 */
interface EmailRule {

    List<Outgoing> apply(Outgoing email);
}
