package com.pousadamarealta;

import static org.junit.jupiter.api.Assertions.*;

import com.pousadamarealta.email.EmailMessage;
import com.pousadamarealta.email.EmailRuleChain;
import org.junit.jupiter.api.Test;

class EmailRuleTest {
    @Test
    void testValidationRuleBlocksInvalidEmail() {
        EmailRuleChain chain = new EmailRuleChain(false, false);
        EmailMessage msg = new EmailMessage("ana@gmail", "Subject", "Body");
        chain.process(msg);
        assertTrue(msg.isBlocked());
    }

    @Test
    void testValidationRuleAllowsValidEmail() {
        EmailRuleChain chain = new EmailRuleChain(false, false);
        EmailMessage msg = new EmailMessage("ana@gmail.com", "Subject", "Body");
        chain.process(msg);
        assertFalse(msg.isBlocked());
    }

    @Test
    void testTestModeRedirect() {
        EmailRuleChain chain = new EmailRuleChain(true, false);
        EmailMessage msg = new EmailMessage("ana@gmail.com", "Subject", "Body");
        chain.process(msg);
        assertEquals("testes@pousadamarealta.com.br", msg.getTo());
        assertTrue(msg.getSubject().startsWith("[TESTE] "));
        assertTrue(msg.getBody().startsWith("Destinatário original: "));
    }

    @Test
    void testArchiveCopyCreated() {
        EmailRuleChain chain = new EmailRuleChain(false, true);
        EmailMessage msg = new EmailMessage("ana@gmail.com", "Subject", "Body");
        chain.process(msg);
        assertEquals(1, chain.getCopies().size());
        EmailMessage copy = chain.getCopies().get(0);
        assertEquals("arquivo@pousadamarealta.com.br", copy.getTo());
        assertTrue(copy.getSubject().startsWith("[Cópia] "));
    }

    @Test
    void testArchiveCopyNotCreatedInTestMode() {
        EmailRuleChain chain = new EmailRuleChain(true, true);
        EmailMessage msg = new EmailMessage("ana@gmail.com", "Subject", "Body");
        chain.process(msg);
        assertEquals(0, chain.getCopies().size());
    }

    @Test
    void testBlockedEmailNoArchiveCopy() {
        EmailRuleChain chain = new EmailRuleChain(false, true);
        EmailMessage msg = new EmailMessage("ana@gmail", "Subject", "Body");
        chain.process(msg);
        assertEquals(0, chain.getCopies().size());
    }
}
