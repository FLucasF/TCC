package hotel.messages;

/** Processa uma mensagem antes de ser enviada: pode bloqueá-la, modificá-la ou criar mensagens adicionais. */
public interface MessageProcessor {

    void process(OutgoingMessage message);
}
