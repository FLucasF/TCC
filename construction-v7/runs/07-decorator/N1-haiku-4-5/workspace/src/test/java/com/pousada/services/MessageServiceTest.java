package com.pousada.services;

import com.pousada.models.Message;
import com.pousada.models.MessageStatus;
import org.junit.Test;
import static org.junit.Assert.*;

public class MessageServiceTest {
    @Test
    public void testValidEmailInProduction() {
        MessageService service = new MessageService(false);
        Message msg = service.sendEmail("user@example.com", "Subject", "Body");
        assertEquals("user@example.com", msg.getTo());
        assertEquals(MessageStatus.SENT, msg.getStatus());
    }

    @Test
    public void testInvalidEmailBlocked() {
        MessageService service = new MessageService(false);
        Message msg = service.sendEmail("invalid-email", "Subject", "Body");
        assertEquals("invalid-email", msg.getTo());
        assertEquals(MessageStatus.BLOCKED, msg.getStatus());
    }

    @Test
    public void testArchiveCopyInProduction() {
        MessageService service = new MessageService(false);
        service.sendEmail("user@example.com", "Subject", "Body");
        java.util.List<Message> messages = service.getMessages();
        assertEquals(2, messages.size());

        Message original = messages.get(0);
        Message copy = messages.get(1);

        assertEquals("user@example.com", original.getTo());
        assertEquals("arquivo@pousadamarealta.com.br", copy.getTo());
        assertEquals("[Cópia] Subject", copy.getSubject());
        assertEquals("Body", copy.getBody());
    }

    @Test
    public void testTestModeRedirection() {
        MessageService service = new MessageService(true);
        Message msg = service.sendEmail("user@example.com", "Subject", "Body");
        assertEquals("testes@pousadamarealta.com.br", msg.getTo());
        assertEquals("[TESTE] Subject", msg.getSubject());
        assertTrue(msg.getBody().startsWith("Destinatário original: user@example.com"));
    }

    @Test
    public void testTestModeNoArchiveCopy() {
        MessageService service = new MessageService(true);
        service.sendEmail("user@example.com", "Subject", "Body");
        java.util.List<Message> messages = service.getMessages();
        assertEquals(1, messages.size());
    }

    @Test
    public void testInvalidEmailNoArchiveCopy() {
        MessageService service = new MessageService(false);
        service.sendEmail("invalid-email", "Subject", "Body");
        java.util.List<Message> messages = service.getMessages();
        assertEquals(1, messages.size());
        assertEquals(MessageStatus.BLOCKED, messages.get(0).getStatus());
    }
}
