package com.pousadamarealta.service;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageStatus;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class MessageServiceTest {
    @Test
    public void testValidEmailIsSent() {
        MessageService service = new MessageService();
        service.sendEmail("user@example.com", "Subject", "Body");

        List<Message> messages = service.getAllMessages();
        assertEquals(2, messages.size()); // Original + Archive copy
        assertEquals("user@example.com", messages.get(0).getTo());
        assertEquals(MessageStatus.SENT, messages.get(0).getStatus());
        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).getTo());
        assertEquals("[Cópia] Subject", messages.get(1).getSubject());
    }

    @Test
    public void testInvalidEmailIsBlocked() {
        MessageService service = new MessageService();
        service.sendEmail("invalid@email", "Subject", "Body");

        List<Message> messages = service.getAllMessages();
        assertEquals(1, messages.size());
        assertEquals(MessageStatus.BLOCKED, messages.get(0).getStatus());
        assertEquals("invalid@email", messages.get(0).getTo());
    }

    @Test
    public void testEmailWithoutAtSignIsBlocked() {
        MessageService service = new MessageService();
        service.sendEmail("invalid.email.com", "Subject", "Body");

        List<Message> messages = service.getAllMessages();
        assertEquals(1, messages.size());
        assertEquals(MessageStatus.BLOCKED, messages.get(0).getStatus());
    }

    @Test
    public void testEmailWithNoDotAfterAtIsBlocked() {
        MessageService service = new MessageService();
        service.sendEmail("user@domain", "Subject", "Body");

        List<Message> messages = service.getAllMessages();
        assertEquals(1, messages.size());
        assertEquals(MessageStatus.BLOCKED, messages.get(0).getStatus());
    }
}
