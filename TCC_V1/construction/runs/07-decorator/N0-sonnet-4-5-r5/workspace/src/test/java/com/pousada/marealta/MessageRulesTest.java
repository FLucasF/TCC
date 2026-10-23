package com.pousada.marealta;

import com.pousada.marealta.domain.Message;
import com.pousada.marealta.domain.MessageStatus;
import com.pousada.marealta.messaging.MessagePipeline;
import com.pousada.marealta.messaging.MessageRule;
import com.pousada.marealta.messaging.rules.ArchiveCopyRule;
import com.pousada.marealta.messaging.rules.TestModeRedirectRule;
import com.pousada.marealta.messaging.rules.ValidateEmailRule;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MessageRulesTest {

    @Test
    void testValidateEmailRule_validEmail() {
        ValidateEmailRule rule = new ValidateEmailRule();
        Message message = new Message("ana@gmail.com", "Test", "Body");

        List<Message> result = rule.process(message);

        assertEquals(1, result.size());
        assertEquals(MessageStatus.SENT, result.get(0).getStatus());
    }

    @Test
    void testValidateEmailRule_invalidEmails() {
        ValidateEmailRule rule = new ValidateEmailRule();

        // Sem @
        Message msg1 = new Message("ana.gmail.com", "Test", "Body");
        assertEquals(MessageStatus.BLOCKED, rule.process(msg1).get(0).getStatus());

        // Múltiplos @
        Message msg2 = new Message("ana@@gmail.com", "Test", "Body");
        assertEquals(MessageStatus.BLOCKED, rule.process(msg2).get(0).getStatus());

        // Sem parte local
        Message msg3 = new Message("@gmail.com", "Test", "Body");
        assertEquals(MessageStatus.BLOCKED, rule.process(msg3).get(0).getStatus());

        // Sem domínio
        Message msg4 = new Message("ana@", "Test", "Body");
        assertEquals(MessageStatus.BLOCKED, rule.process(msg4).get(0).getStatus());

        // Sem ponto no domínio
        Message msg5 = new Message("ana@gmail", "Test", "Body");
        assertEquals(MessageStatus.BLOCKED, rule.process(msg5).get(0).getStatus());
    }

    @Test
    void testTestModeRedirectRule_productionMode() {
        TestModeRedirectRule rule = new TestModeRedirectRule(false);
        Message message = new Message("ana@gmail.com", "Subject", "Body");

        List<Message> result = rule.process(message);

        assertEquals(1, result.size());
        assertEquals("ana@gmail.com", result.get(0).getTo());
        assertEquals("Subject", result.get(0).getSubject());
        assertEquals("Body", result.get(0).getBody());
    }

    @Test
    void testTestModeRedirectRule_testMode() {
        TestModeRedirectRule rule = new TestModeRedirectRule(true);
        Message message = new Message("ana@gmail.com", "Subject", "Body");

        List<Message> result = rule.process(message);

        assertEquals(1, result.size());
        assertEquals("testes@pousadamarealta.com.br", result.get(0).getTo());
        assertEquals("[TESTE] Subject", result.get(0).getSubject());
        assertTrue(result.get(0).getBody().startsWith("Destinatário original: ana@gmail.com\n"));
    }

    @Test
    void testTestModeRedirectRule_blockedMessageNotRedirected() {
        TestModeRedirectRule rule = new TestModeRedirectRule(true);
        Message message = new Message("invalid", "Subject", "Body");
        message.setStatus(MessageStatus.BLOCKED);

        List<Message> result = rule.process(message);

        assertEquals(1, result.size());
        assertEquals("invalid", result.get(0).getTo());
        assertEquals(MessageStatus.BLOCKED, result.get(0).getStatus());
    }

    @Test
    void testArchiveCopyRule_enabled() {
        ArchiveCopyRule rule = new ArchiveCopyRule(true);
        Message message = new Message("ana@gmail.com", "Subject", "Body");

        List<Message> result = rule.process(message);

        assertEquals(2, result.size());

        // Original
        assertEquals("ana@gmail.com", result.get(0).getTo());
        assertEquals("Subject", result.get(0).getSubject());

        // Cópia
        assertEquals("arquivo@pousadamarealta.com.br", result.get(1).getTo());
        assertEquals("[Cópia] Subject", result.get(1).getSubject());
        assertEquals("Body", result.get(1).getBody());
    }

    @Test
    void testArchiveCopyRule_disabled() {
        ArchiveCopyRule rule = new ArchiveCopyRule(false);
        Message message = new Message("ana@gmail.com", "Subject", "Body");

        List<Message> result = rule.process(message);

        assertEquals(1, result.size());
        assertEquals("ana@gmail.com", result.get(0).getTo());
    }

    @Test
    void testArchiveCopyRule_blockedMessageNoCopy() {
        ArchiveCopyRule rule = new ArchiveCopyRule(true);
        Message message = new Message("invalid", "Subject", "Body");
        message.setStatus(MessageStatus.BLOCKED);

        List<Message> result = rule.process(message);

        assertEquals(1, result.size());
        assertEquals("invalid", result.get(0).getTo());
    }

    @Test
    void testMessagePipeline_productionMode() {
        List<MessageRule> rules = new ArrayList<>();
        rules.add(new ValidateEmailRule());
        rules.add(new TestModeRedirectRule(false));
        rules.add(new ArchiveCopyRule(true));

        MessagePipeline pipeline = new MessagePipeline(rules);
        Message message = new Message("ana@gmail.com", "Subject", "Body");

        List<Message> result = pipeline.process(message);

        // Mensagem original + cópia para arquivo
        assertEquals(2, result.size());
        assertEquals("ana@gmail.com", result.get(0).getTo());
        assertEquals("arquivo@pousadamarealta.com.br", result.get(1).getTo());
    }

    @Test
    void testMessagePipeline_testMode() {
        List<MessageRule> rules = new ArrayList<>();
        rules.add(new ValidateEmailRule());
        rules.add(new TestModeRedirectRule(true));
        rules.add(new ArchiveCopyRule(false));

        MessagePipeline pipeline = new MessagePipeline(rules);
        Message message = new Message("ana@gmail.com", "Subject", "Body");

        List<Message> result = pipeline.process(message);

        // Apenas mensagem redirecionada (sem cópia em modo teste)
        assertEquals(1, result.size());
        assertEquals("testes@pousadamarealta.com.br", result.get(0).getTo());
        assertEquals("[TESTE] Subject", result.get(0).getSubject());
    }

    @Test
    void testMessagePipeline_invalidEmailBlocked() {
        List<MessageRule> rules = new ArrayList<>();
        rules.add(new ValidateEmailRule());
        rules.add(new TestModeRedirectRule(true));
        rules.add(new ArchiveCopyRule(true));

        MessagePipeline pipeline = new MessagePipeline(rules);
        Message message = new Message("ana.gmail.com", "Subject", "Body");

        List<Message> result = pipeline.process(message);

        // Mensagem bloqueada, não redirecionada, sem cópia
        assertEquals(1, result.size());
        assertEquals("ana.gmail.com", result.get(0).getTo());
        assertEquals(MessageStatus.BLOCKED, result.get(0).getStatus());
    }
}
