package com.pousada;

import com.pousada.models.Message;
import com.pousada.services.MessageService;
import org.junit.Test;
import static org.junit.Assert.*;

public class MessageServiceTest {
    @Test
    public void testValidEmailSent() {
        MessageService service = new MessageService(false, true);
        service.sendMessage("test@example.com", "Subject", "Body");

        assertEquals(2, service.getMessages().size());

        Message original = service.getMessages().get(0);
        assertEquals("test@example.com", original.getTo());
        assertEquals("Subject", original.getSubject());
        assertEquals("Body", original.getBody());
        assertEquals(Message.Status.SENT, original.getStatus());

        Message archive = service.getMessages().get(1);
        assertEquals("arquivo@pousadamarealta.com.br", archive.getTo());
        assertEquals("[Cópia] Subject", archive.getSubject());
        assertEquals("Body", archive.getBody());
        assertEquals(Message.Status.SENT, archive.getStatus());
    }

    @Test
    public void testInvalidEmailBlocked() {
        MessageService service = new MessageService(false, true);
        service.sendMessage("invalid-email", "Subject", "Body");

        assertEquals(1, service.getMessages().size());

        Message blocked = service.getMessages().get(0);
        assertEquals("invalid-email", blocked.getTo());
        assertEquals("Subject", blocked.getSubject());
        assertEquals("Body", blocked.getBody());
        assertEquals(Message.Status.BLOCKED, blocked.getStatus());
    }

    @Test
    public void testTestMode() {
        MessageService service = new MessageService(true, true);
        service.sendMessage("test@example.com", "Subject", "Body");

        assertEquals(1, service.getMessages().size());

        Message testMessage = service.getMessages().get(0);
        assertEquals("testes@pousadamarealta.com.br", testMessage.getTo());
        assertEquals("[TESTE] Subject", testMessage.getSubject());
        assertTrue(testMessage.getBody().contains("Destinatário original: test@example.com"));
        assertEquals(Message.Status.SENT, testMessage.getStatus());
    }

    @Test
    public void testTestModeNoArchive() {
        MessageService service = new MessageService(true, true);
        service.sendMessage("test@example.com", "Subject", "Body");

        assertEquals(1, service.getMessages().size());
    }

    @Test
    public void testNoArchiveWhenDisabled() {
        MessageService service = new MessageService(false, false);
        service.sendMessage("test@example.com", "Subject", "Body");

        assertEquals(1, service.getMessages().size());
        assertEquals("test@example.com", service.getMessages().get(0).getTo());
    }
}
