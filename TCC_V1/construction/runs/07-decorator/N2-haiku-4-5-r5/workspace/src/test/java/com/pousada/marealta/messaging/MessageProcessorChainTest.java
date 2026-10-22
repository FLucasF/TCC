package com.pousada.marealta.messaging;

import com.pousada.marealta.domain.MessageStatus;
import com.pousada.marealta.messaging.processors.EmailValidationProcessor;
import com.pousada.marealta.messaging.processors.TestModeProcessor;
import org.junit.Assert;
import org.junit.Test;

public class MessageProcessorChainTest {
    @Test
    public void testInvalidEmailBlocked() {
        MessageProcessorChain chain = new MessageProcessorChain();
        chain.addProcessor(new EmailValidationProcessor(true));

        MessageData message = new MessageData("invalid-email", "Subject", "Body");
        chain.process(message);

        Assert.assertEquals(MessageStatus.BLOCKED, message.getStatus());
    }

    @Test
    public void testValidEmailAllowed() {
        MessageProcessorChain chain = new MessageProcessorChain();
        chain.addProcessor(new EmailValidationProcessor(true));

        MessageData message = new MessageData("user@example.com", "Subject", "Body");
        chain.process(message);

        Assert.assertEquals(MessageStatus.SENT, message.getStatus());
    }

    @Test
    public void testTestModeRedirection() {
        MessageProcessorChain chain = new MessageProcessorChain();
        chain.addProcessor(new EmailValidationProcessor(true));
        chain.addProcessor(new TestModeProcessor(true));

        MessageData message = new MessageData("user@example.com", "Confirmação", "Seu pedido");
        chain.process(message);

        Assert.assertEquals("testes@pousadamarealta.com.br", message.getTo());
        Assert.assertTrue(message.getSubject().startsWith("[TESTE]"));
        Assert.assertTrue(message.getBody().contains("Destinatário original: user@example.com"));
        Assert.assertFalse(message.shouldGenerateCopy());
    }

    @Test
    public void testBlockedMessageDoesNotProcessFurther() {
        MessageProcessorChain chain = new MessageProcessorChain();
        chain.addProcessor(new EmailValidationProcessor(true));
        chain.addProcessor(new TestModeProcessor(true));

        MessageData message = new MessageData("invalid@email", "Subject", "Body");
        chain.process(message);

        Assert.assertEquals(MessageStatus.BLOCKED, message.getStatus());
        Assert.assertEquals("invalid@email", message.getTo());
        Assert.assertFalse(message.getSubject().startsWith("[TESTE]"));
    }
}
