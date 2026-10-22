package com.pousadamarealta.service;

import com.pousadamarealta.db.MessageRepository;
import com.pousadamarealta.model.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MessageServiceTest {
    private MessageRepository messageRepository;
    private MessageService messageService;

    @BeforeEach
    public void setUp() {
        messageRepository = new MockMessageRepository();
        messageService = new MessageService(messageRepository);
    }

    @Test
    @DisplayName("Valid email should be accepted")
    public void testValidEmail() throws Exception {
        List<Message> messages = messageService.sendMessage("user@example.com", "Subject", "Body");
        assertTrue(messages.stream().anyMatch(m -> "SENT".equals(m.getStatus())));
    }

    @Test
    @DisplayName("Email without @ should be blocked")
    public void testEmailWithoutAt() throws Exception {
        List<Message> messages = messageService.sendMessage("usergmail.com", "Subject", "Body");
        assertTrue(messages.stream().anyMatch(m -> "BLOCKED".equals(m.getStatus())));
    }

    @Test
    @DisplayName("Email without domain should be blocked")
    public void testEmailWithoutDomain() throws Exception {
        List<Message> messages = messageService.sendMessage("user@gmail", "Subject", "Body");
        assertTrue(messages.stream().anyMatch(m -> "BLOCKED".equals(m.getStatus())));
    }

    @Test
    @DisplayName("Email with multiple @ should be blocked")
    public void testEmailWithMultipleAt() throws Exception {
        List<Message> messages = messageService.sendMessage("user@@gmail.com", "Subject", "Body");
        assertTrue(messages.stream().anyMatch(m -> "BLOCKED".equals(m.getStatus())));
    }

    private static class MockMessageRepository extends MessageRepository {
        private List<Message> messages = new ArrayList<>();

        public MockMessageRepository() {
            super(null);
        }

        @Override
        public Message save(Message message) {
            if (message.getId() == null) {
                message.setId((long) messages.size() + 1);
            }
            messages.add(message);
            return message;
        }

        @Override
        public List<Message> findAll() {
            return new ArrayList<>(messages);
        }
    }
}
