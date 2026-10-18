package hotel.messages;

import java.util.Collections;
import java.util.List;

/**
 * In test mode, redirects emails to a test address with a warning prefix in the subject and body.
 */
public final class TestModeRedirectionRule implements EmailRule {

    private static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";
    private static final String SUBJECT_PREFIX = "[TESTE] ";

    private final boolean testMode;

    public TestModeRedirectionRule(boolean testMode) {
        this.testMode = testMode;
    }

    @Override
    public List<OutgoingEmail> process(OutgoingEmail email) {
        if (!testMode || email.status() == MessageStatus.BLOCKED) {
            return Collections.emptyList();
        }

        email.setTo(TEST_ADDRESS);
        email.setSubject(SUBJECT_PREFIX + email.subject());
        email.setBody("Destinatário original: " + email.originalTo() + "\n" + email.body());

        return Collections.emptyList();
    }
}
