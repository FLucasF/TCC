package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.MessageStatus;
import com.pousadamarealta.messaging.rules.ArchiveCopyRule;
import com.pousadamarealta.messaging.rules.EmailValidationRule;
import com.pousadamarealta.messaging.rules.TestModeRedirectRule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MessagePipelineTest {

    @Test
    public void shouldBlockInvalidEmail() {
        MessagePipeline pipeline = new MessagePipeline(List.of(new EmailValidationRule()));

        MessageRequest request = new MessageRequest(
            MessageChannel.EMAIL,
            "ana.gmail.com",
            "Test",
            "Body"
        );

        MessagePipeline.ProcessingResult result = pipeline.process(request);

        assertEquals(1, result.getMessages().size());
        Message message = result.getMessages().get(0);
        assertEquals(MessageStatus.BLOCKED, message.getStatus());
        assertEquals("ana.gmail.com", message.getTo());
    }

    @Test
    public void shouldAllowValidEmail() {
        MessagePipeline pipeline = new MessagePipeline(List.of(new EmailValidationRule()));

        MessageRequest request = new MessageRequest(
            MessageChannel.EMAIL,
            "ana@gmail.com",
            "Test",
            "Body"
        );

        MessagePipeline.ProcessingResult result = pipeline.process(request);

        assertEquals(1, result.getMessages().size());
        Message message = result.getMessages().get(0);
        assertEquals(MessageStatus.SENT, message.getStatus());
        assertEquals("ana@gmail.com", message.getTo());
    }

    @Test
    public void shouldRedirectInTestMode() {
        MessagePipeline pipeline = new MessagePipeline(List.of(
            new EmailValidationRule(),
            new TestModeRedirectRule()
        ));

        MessageRequest request = new MessageRequest(
            MessageChannel.EMAIL,
            "ana@gmail.com",
            "Reserva confirmada",
            "Olá Ana!"
        );

        MessagePipeline.ProcessingResult result = pipeline.process(request);

        assertEquals(1, result.getMessages().size());
        Message message = result.getMessages().get(0);
        assertEquals(MessageStatus.SENT, message.getStatus());
        assertEquals("testes@pousadamarealta.com.br", message.getTo());
        assertTrue(message.getSubject().startsWith("[TESTE] "));
        assertTrue(message.getBody().contains("Destinatário original: ana@gmail.com"));
    }

    @Test
    public void shouldCreateArchiveCopy() {
        MessagePipeline pipeline = new MessagePipeline(List.of(
            new EmailValidationRule(),
            new ArchiveCopyRule()
        ));

        MessageRequest request = new MessageRequest(
            MessageChannel.EMAIL,
            "ana@gmail.com",
            "Reserva confirmada",
            "Olá Ana!"
        );

        MessagePipeline.ProcessingResult result = pipeline.process(request);

        assertEquals(2, result.getMessages().size());

        Message original = result.getMessages().get(0);
        assertEquals(MessageStatus.SENT, original.getStatus());
        assertEquals("ana@gmail.com", original.getTo());
        assertEquals("Reserva confirmada", original.getSubject());

        Message copy = result.getMessages().get(1);
        assertEquals(MessageStatus.SENT, copy.getStatus());
        assertEquals("arquivo@pousadamarealta.com.br", copy.getTo());
        assertTrue(copy.getSubject().startsWith("[Cópia] "));
        assertEquals("Olá Ana!", copy.getBody());
    }

    @Test
    public void shouldNotCreateArchiveCopyForBlockedMessage() {
        MessagePipeline pipeline = new MessagePipeline(List.of(
            new EmailValidationRule(),
            new ArchiveCopyRule()
        ));

        MessageRequest request = new MessageRequest(
            MessageChannel.EMAIL,
            "ana.gmail.com",
            "Test",
            "Body"
        );

        MessagePipeline.ProcessingResult result = pipeline.process(request);

        assertEquals(1, result.getMessages().size());
        Message message = result.getMessages().get(0);
        assertEquals(MessageStatus.BLOCKED, message.getStatus());
    }

    @Test
    public void shouldBlockInvalidEmailInTestMode() {
        MessagePipeline pipeline = new MessagePipeline(List.of(
            new EmailValidationRule(),
            new TestModeRedirectRule()
        ));

        MessageRequest request = new MessageRequest(
            MessageChannel.EMAIL,
            "ana.gmail.com",
            "Test",
            "Body"
        );

        MessagePipeline.ProcessingResult result = pipeline.process(request);

        assertEquals(1, result.getMessages().size());
        Message message = result.getMessages().get(0);
        assertEquals(MessageStatus.BLOCKED, message.getStatus());
        assertEquals("ana.gmail.com", message.getTo());
    }
}
