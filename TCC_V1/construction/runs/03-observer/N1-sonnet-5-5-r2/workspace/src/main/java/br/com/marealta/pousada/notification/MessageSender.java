package br.com.marealta.pousada.notification;

/** Por onde saem as mensagens. Hoje elas só ficam guardadas; depois podem sair de verdade. */
public interface MessageSender {

    void send(Channel channel, String to, String subject, String body);
}
