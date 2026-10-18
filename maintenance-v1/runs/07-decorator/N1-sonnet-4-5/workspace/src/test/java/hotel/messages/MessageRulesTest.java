package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MessageRulesTest {

    @Test
    void emailValidationRuleBlocksInvalidEmails() {
        EmailValidationRule rule = new EmailValidationRule();
        MessageToSend valid = new MessageToSend("ana@example.com", "Subject", "Body", MessageStatus.SENT);
        MessageToSend invalid = new MessageToSend("ana@invalid", "Subject", "Body", MessageStatus.SENT);

        assertEquals(MessageStatus.SENT, rule.apply(valid, false).status());
        assertEquals(MessageStatus.BLOCKED, rule.apply(invalid, false).status());
    }

    @Test
    void testModeRedirectRuleRedirectsInTestMode() {
        TestModeRedirectRule rule = new TestModeRedirectRule();
        MessageToSend message = new MessageToSend("ana@example.com", "Subject", "Body", MessageStatus.SENT);

        MessageToSend result = rule.apply(message, true);

        assertEquals("testes@pousadamarealta.com.br", result.to());
        assertEquals("[TESTE] Subject", result.subject());
        assertTrue(result.body().startsWith("Destinatário original: ana@example.com\n"));
        assertTrue(result.body().contains("Body"));
    }

    @Test
    void testModeRedirectRuleDoesNotRedirectInProductionMode() {
        TestModeRedirectRule rule = new TestModeRedirectRule();
        MessageToSend message = new MessageToSend("ana@example.com", "Subject", "Body", MessageStatus.SENT);

        MessageToSend result = rule.apply(message, false);

        assertEquals("ana@example.com", result.to());
        assertEquals("Subject", result.subject());
        assertEquals("Body", result.body());
    }

    @Test
    void testModeRedirectRuleDoesNotRedirectBlockedMessages() {
        TestModeRedirectRule rule = new TestModeRedirectRule();
        MessageToSend blocked = new MessageToSend("ana@invalid", "Subject", "Body", MessageStatus.BLOCKED);

        MessageToSend result = rule.apply(blocked, true);

        assertEquals("ana@invalid", result.to());
        assertEquals(MessageStatus.BLOCKED, result.status());
    }

    @Test
    void archiveCopyRuleCreatesArchiveCopyInProductionMode() {
        ArchiveCopyRule rule = new ArchiveCopyRule();
        MessageToSend message = new MessageToSend("ana@example.com", "Subject", "Body", MessageStatus.SENT);

        MessageToSend result = rule.apply(message, false);

        assertEquals(1, result.additionalMessages().size());
        MessageToSend archive = result.additionalMessages().get(0);
        assertEquals("arquivo@pousadamarealta.com.br", archive.to());
        assertEquals("[Cópia] Subject", archive.subject());
        assertEquals("Body", archive.body());
        assertEquals(MessageStatus.SENT, archive.status());
    }

    @Test
    void archiveCopyRuleDoesNotCreateCopyInTestMode() {
        ArchiveCopyRule rule = new ArchiveCopyRule();
        MessageToSend message = new MessageToSend("ana@example.com", "Subject", "Body", MessageStatus.SENT);

        MessageToSend result = rule.apply(message, true);

        assertEquals(0, result.additionalMessages().size());
    }

    @Test
    void archiveCopyRuleDoesNotCreateCopyForBlockedMessages() {
        ArchiveCopyRule rule = new ArchiveCopyRule();
        MessageToSend blocked = new MessageToSend("ana@invalid", "Subject", "Body", MessageStatus.BLOCKED);

        MessageToSend result = rule.apply(blocked, false);

        assertEquals(0, result.additionalMessages().size());
    }
}
