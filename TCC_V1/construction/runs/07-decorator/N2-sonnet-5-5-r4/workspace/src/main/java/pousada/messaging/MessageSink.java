package pousada.messaging;

/** Destino de uma mensagem. Devolve a mensagem como ficou registrada (com o status final). */
public interface MessageSink {
    Message send(Message message);
}
