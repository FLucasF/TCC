package br.com.pousadamarealta.reservas.notification;

/** Por onde as mensagens saem. Hoje só as guarda na lista de enviadas. */
public interface MessageGateway {
    void sendEmail(String to, String subject, String body);
}
