package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MessageHandlerTest {

    @Test
    void validationHandlerBlocksInvalidEmail() {
        MessageRequest request = new MessageRequest("ana@gmail", "Test", "Body");
        new ValidationHandler().handle(request);

        assertEquals(MessageStatus.BLOCKED, request.status());
    }

    @Test
    void validationHandlerAllowsValidEmail() {
        MessageRequest request = new MessageRequest("ana@gmail.com", "Test", "Body");
        new ValidationHandler().handle(request);

        assertEquals(MessageStatus.SENT, request.status());
    }

    @Test
    void testModeHandlerRedirectsEmail() {
        MessageRequest request = new MessageRequest("user@domain.com", "Subject", "Body");
        new ValidationHandler().handle(request);
        new TestModeHandler(true).handle(request);

        assertEquals("testes@pousadamarealta.com.br", request.to());
        assertEquals("[TESTE] Subject", request.subject());
        assertEquals("Destinatário original: user@domain.com\nBody", request.body());
    }

    @Test
    void testModeHandlerDoesNotRedirectBlockedEmail() {
        MessageRequest request = new MessageRequest("ana@gmail", "Subject", "Body");
        new ValidationHandler().handle(request);
        new TestModeHandler(true).handle(request);

        assertEquals("ana@gmail", request.to());
        assertEquals("Subject", request.subject());
        assertEquals(MessageStatus.BLOCKED, request.status());
    }

    @Test
    void archiveCopyHandlerGeneratesCopy() {
        MessageRequest request = new MessageRequest("user@domain.com", "Subject", "Body");
        new ValidationHandler().handle(request);
        new TestModeHandler(false).handle(request);
        new ArchiveCopyHandler(false).handle(request);

        assertEquals(1, request.generatedMessages().size());
        Message copy = request.generatedMessages().get(0);
        assertEquals("arquivo@pousadamarealta.com.br", copy.to());
        assertEquals("[Cópia] Subject", copy.subject());
    }

    @Test
    void archiveCopyHandlerDoesNotGenerateCopyInTestMode() {
        MessageRequest request = new MessageRequest("user@domain.com", "Subject", "Body");
        new ValidationHandler().handle(request);
        new TestModeHandler(true).handle(request);
        new ArchiveCopyHandler(true).handle(request);

        assertEquals(0, request.generatedMessages().size());
    }

    @Test
    void archiveCopyHandlerDoesNotGenerateCopyForBlockedEmail() {
        MessageRequest request = new MessageRequest("ana@gmail", "Subject", "Body");
        new ValidationHandler().handle(request);
        new TestModeHandler(false).handle(request);
        new ArchiveCopyHandler(false).handle(request);

        assertEquals(0, request.generatedMessages().size());
    }

    @Test
    void fullChainWithValidEmail() {
        MessageRequest request = new MessageRequest("user@domain.com", "Subject", "Body");
        new ValidationHandler().handle(request);
        new TestModeHandler(false).handle(request);
        new ArchiveCopyHandler(false).handle(request);

        assertEquals(MessageStatus.SENT, request.status());
        assertEquals("user@domain.com", request.to());
        assertEquals("Subject", request.subject());
        assertEquals(1, request.generatedMessages().size());
    }

    @Test
    void fullChainWithInvalidEmail() {
        MessageRequest request = new MessageRequest("ana@gmail", "Subject", "Body");
        new ValidationHandler().handle(request);
        new TestModeHandler(false).handle(request);
        new ArchiveCopyHandler(false).handle(request);

        assertEquals(MessageStatus.BLOCKED, request.status());
        assertEquals("ana@gmail", request.to());
        assertEquals(0, request.generatedMessages().size());
    }
}
