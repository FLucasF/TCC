package br.com.marealta.notification;

public interface MessageSender {

    void send(Channel channel, String to, String subject, String body);

    default void email(String to, String subject, String body) {
        send(Channel.EMAIL, to, subject, body);
    }
}
