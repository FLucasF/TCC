package br.com.marealta.messaging;

/** Fim da fila: por enquanto o "envio" é guardar na lista de mensagens. */
public class LogSender implements MessageSender {
    private final MessageLog log;

    public LogSender(MessageLog log) { this.log = log; }

    @Override
    public MessageStatus send(Message message) {
        log.save(message, MessageStatus.SENT);
        return MessageStatus.SENT;
    }
}
