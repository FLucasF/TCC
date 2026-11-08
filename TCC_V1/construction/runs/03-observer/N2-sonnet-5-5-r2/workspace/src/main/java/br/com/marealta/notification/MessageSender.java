package br.com.marealta.notification;

/** Por onde as mensagens saem. Por enquanto elas só ficam guardadas. */
public interface MessageSender {
    void send(Message message);
}
