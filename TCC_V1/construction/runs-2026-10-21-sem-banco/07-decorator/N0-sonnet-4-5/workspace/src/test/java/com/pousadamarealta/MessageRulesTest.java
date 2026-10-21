package com.pousadamarealta;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.MessageStatus;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.service.MessageService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MessageRulesTest {

    @Test
    public void testEmailValidation_ValidEmail() {
        MessageRepository repository = new MessageRepository();
        MessageService service = new MessageService(repository, false);

        service.sendMessage(MessageChannel.EMAIL, "ana@gmail.com", "Test", "Body");

        List<Message> messages = repository.findAll();
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getStatus()).isEqualTo(MessageStatus.SENT);
        assertThat(messages.get(0).getTo()).isEqualTo("ana@gmail.com");
    }

    @Test
    public void testEmailValidation_InvalidEmail_NoAt() {
        MessageRepository repository = new MessageRepository();
        MessageService service = new MessageService(repository, false);

        service.sendMessage(MessageChannel.EMAIL, "ana.gmail.com", "Test", "Body");

        List<Message> messages = repository.findAll();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getStatus()).isEqualTo(MessageStatus.BLOCKED);
        assertThat(messages.get(0).getTo()).isEqualTo("ana.gmail.com");
    }

    @Test
    public void testEmailValidation_InvalidEmail_NoDomain() {
        MessageRepository repository = new MessageRepository();
        MessageService service = new MessageService(repository, false);

        service.sendMessage(MessageChannel.EMAIL, "ana@gmail", "Test", "Body");

        List<Message> messages = repository.findAll();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getStatus()).isEqualTo(MessageStatus.BLOCKED);
        assertThat(messages.get(0).getTo()).isEqualTo("ana@gmail");
    }

    @Test
    public void testArchiveCopy_ProductionMode() {
        MessageRepository repository = new MessageRepository();
        MessageService service = new MessageService(repository, false);

        service.sendMessage(MessageChannel.EMAIL, "ana@gmail.com", "Test Subject", "Test Body");

        List<Message> messages = repository.findAll();
        assertThat(messages).hasSize(2);

        assertThat(messages.get(0).getTo()).isEqualTo("ana@gmail.com");
        assertThat(messages.get(0).getSubject()).isEqualTo("Test Subject");
        assertThat(messages.get(0).getStatus()).isEqualTo(MessageStatus.SENT);

        assertThat(messages.get(1).getTo()).isEqualTo("arquivo@pousadamarealta.com.br");
        assertThat(messages.get(1).getSubject()).isEqualTo("[Cópia] Test Subject");
        assertThat(messages.get(1).getBody()).isEqualTo("Test Body");
        assertThat(messages.get(1).getStatus()).isEqualTo(MessageStatus.SENT);
    }

    @Test
    public void testTestMode_RedirectsToTestEmail() {
        MessageRepository repository = new MessageRepository();
        MessageService service = new MessageService(repository, true);

        service.sendMessage(MessageChannel.EMAIL, "ana@gmail.com", "Test Subject", "Test Body");

        List<Message> messages = repository.findAll();
        assertThat(messages).hasSize(1);

        assertThat(messages.get(0).getTo()).isEqualTo("testes@pousadamarealta.com.br");
        assertThat(messages.get(0).getSubject()).isEqualTo("[TESTE] Test Subject");
        assertThat(messages.get(0).getBody()).startsWith("Destinatário original: ana@gmail.com\n");
        assertThat(messages.get(0).getStatus()).isEqualTo(MessageStatus.SENT);
    }

    @Test
    public void testTestMode_BlockedEmailStaysBlocked() {
        MessageRepository repository = new MessageRepository();
        MessageService service = new MessageService(repository, true);

        service.sendMessage(MessageChannel.EMAIL, "invalid-email", "Test Subject", "Test Body");

        List<Message> messages = repository.findAll();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getStatus()).isEqualTo(MessageStatus.BLOCKED);
        assertThat(messages.get(0).getTo()).isEqualTo("invalid-email");
    }

    @Test
    public void testTestMode_NoArchiveCopy() {
        MessageRepository repository = new MessageRepository();
        MessageService service = new MessageService(repository, true);

        service.sendMessage(MessageChannel.EMAIL, "ana@gmail.com", "Test", "Body");

        List<Message> messages = repository.findAll();
        assertThat(messages).hasSize(1);
        assertThat(messages.stream().noneMatch(m -> m.getTo().contains("arquivo"))).isTrue();
    }

    @Test
    public void testProductionMode_BlockedEmailNoArchiveCopy() {
        MessageRepository repository = new MessageRepository();
        MessageService service = new MessageService(repository, false);

        service.sendMessage(MessageChannel.EMAIL, "invalid-email", "Test", "Body");

        List<Message> messages = repository.findAll();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getStatus()).isEqualTo(MessageStatus.BLOCKED);
    }
}
