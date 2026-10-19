package com.pousadamarealta;

import com.pousadamarealta.messaging.Message;
import com.pousadamarealta.messaging.MessageService;
import com.pousadamarealta.messaging.MessageStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MessageServiceTest {

    @Test
    void testValidEmailSentInNormalMode() {
        MessageService service = new MessageService(false);
        service.send("ana@gmail.com", "Test", "Body");

        List<Message> messages = service.getSentMessages();
        assertEquals(2, messages.size()); // Original + cópia para arquivo

        Message original = messages.get(0);
        assertEquals("ana@gmail.com", original.getTo());
        assertEquals("Test", original.getSubject());
        assertEquals(MessageStatus.SENT, original.getStatus());

        Message archive = messages.get(1);
        assertEquals("arquivo@pousadamarealta.com.br", archive.getTo());
        assertEquals("[Cópia] Test", archive.getSubject());
        assertEquals(MessageStatus.SENT, archive.getStatus());
    }

    @Test
    void testInvalidEmailBlocked() {
        MessageService service = new MessageService(false);
        service.send("ana@gmail", "Test", "Body");

        List<Message> messages = service.getSentMessages();
        assertEquals(1, messages.size()); // Apenas mensagem bloqueada, sem cópia

        Message blocked = messages.get(0);
        assertEquals("ana@gmail", blocked.getTo());
        assertEquals(MessageStatus.BLOCKED, blocked.getStatus());
    }

    @Test
    void testTestModeRedirect() {
        MessageService service = new MessageService(true);
        service.send("ana@gmail.com", "Test", "Body");

        List<Message> messages = service.getSentMessages();
        assertEquals(1, messages.size()); // Sem cópia em modo de teste

        Message redirected = messages.get(0);
        assertEquals("testes@pousadamarealta.com.br", redirected.getTo());
        assertEquals("[TESTE] Test", redirected.getSubject());
        assertTrue(redirected.getBody().startsWith("Destinatário original: ana@gmail.com"));
        assertEquals(MessageStatus.SENT, redirected.getStatus());
    }

    @Test
    void testTestModeBlocksInvalidEmail() {
        MessageService service = new MessageService(true);
        service.send("invalid", "Test", "Body");

        List<Message> messages = service.getSentMessages();
        assertEquals(1, messages.size());

        Message blocked = messages.get(0);
        assertEquals("invalid", blocked.getTo());
        assertEquals(MessageStatus.BLOCKED, blocked.getStatus());
    }
}
