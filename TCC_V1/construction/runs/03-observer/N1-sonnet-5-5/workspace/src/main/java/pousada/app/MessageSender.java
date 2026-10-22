package pousada.app;

public interface MessageSender {
    void send(Message.Channel channel, String to, String subject, String body);
}
