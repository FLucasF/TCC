package com.pousadamarealta;

import com.pousadamarealta.model.Message;
import com.pousadamarealta.model.MessageStatus;
import com.pousadamarealta.service.MessageService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MessageServiceTest {
    @Test
    public void testValidEmailSent() {
        MessageService service = new MessageService(false);
        Message message = service.sendEmail("user@example.com", "Test", "Body");

        assertEquals(MessageStatus.SENT, message.getStatus());
        assertEquals("user@example.com", message.getTo());

        List<Message> messages = service.getMessages();
        assertEquals(2, messages.size());
        assertEquals("user@example.com", messages.get(0).getTo());
        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).getTo());
        assertEquals("[Cópia] Test", messages.get(1).getSubject());
    }

    @Test
    public void testInvalidEmailBlocked() {
        MessageService service = new MessageService(false);
        Message message = service.sendEmail("invalid@gmail", "Test", "Body");

        assertEquals(MessageStatus.BLOCKED, message.getStatus());
        assertEquals("invalid@gmail", message.getTo());

        List<Message> messages = service.getMessages();
        assertEquals(1, messages.size());
    }

    @Test
    public void testTestModeRedirect() {
        MessageService service = new MessageService(true);
        Message message = service.sendEmail("user@example.com", "Test", "Body");

        assertEquals(MessageStatus.SENT, message.getStatus());
        assertEquals("testes@pousadamarealta.com.br", message.getTo());
        assertEquals("[TESTE] Test", message.getSubject());
        assertTrue(message.getBody().contains("Destinatário original: user@example.com"));

        List<Message> messages = service.getMessages();
        assertEquals(1, messages.size());
    }

    @Test
    public void testTestModeInvalidEmailStillBlocked() {
        MessageService service = new MessageService(true);
        Message message = service.sendEmail("invalid@gmail", "Test", "Body");

        assertEquals(MessageStatus.BLOCKED, message.getStatus());
        assertEquals("invalid@gmail", message.getTo());

        List<Message> messages = service.getMessages();
        assertEquals(1, messages.size());
    }
}
