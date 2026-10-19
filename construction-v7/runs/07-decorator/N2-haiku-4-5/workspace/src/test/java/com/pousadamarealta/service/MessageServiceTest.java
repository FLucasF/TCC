package com.pousadamarealta.service;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageStatus;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class MessageServiceTest {

    @Test
    public void testSendValidEmail() {
        MessageService service = new MessageService();
        Message msg = service.sendMessage("john@example.com", "Test", "Body");

        assertNotNull(msg);
        assertEquals("john@example.com", msg.getTo());
        assertEquals(MessageStatus.SENT, msg.getStatus());
    }

    @Test
    public void testSendInvalidEmailBlockedNoAt() {
        MessageService service = new MessageService();
        Message msg = service.sendMessage("johngmail.com", "Test", "Body");

        assertNotNull(msg);
        assertEquals("johngmail.com", msg.getTo());
        assertEquals(MessageStatus.BLOCKED, msg.getStatus());
    }

    @Test
    public void testSendInvalidEmailBlockedNoDot() {
        MessageService service = new MessageService();
        Message msg = service.sendMessage("john@gmail", "Test", "Body");

        assertNotNull(msg);
        assertEquals("john@gmail", msg.getTo());
        assertEquals(MessageStatus.BLOCKED, msg.getStatus());
    }

    @Test
    public void testValidEmailCreatesArchiveCopy() {
        MessageService service = new MessageService();
        service.sendMessage("john@example.com", "Test", "Body");

        List<Message> messages = service.getAllMessages();
        assertEquals(2, messages.size());

        Message original = messages.get(0);
        Message copy = messages.get(1);

        assertEquals("john@example.com", original.getTo());
        assertEquals("arquivo@pousadamarealta.com.br", copy.getTo());
        assertEquals("[Cópia] Test", copy.getSubject());
        assertEquals("Body", copy.getBody());
        assertEquals(MessageStatus.SENT, copy.getStatus());
    }

    @Test
    public void testBlockedEmailNoArchiveCopy() {
        MessageService service = new MessageService();
        service.sendMessage("invalidemail", "Test", "Body");

        List<Message> messages = service.getAllMessages();
        assertEquals(1, messages.size());
        assertEquals(MessageStatus.BLOCKED, messages.get(0).getStatus());
    }
}
