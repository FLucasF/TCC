package br.com.marealta.app;

/** Uma mensagem já enviada (guardada na lista de enviadas). */
public record Message(long id, Channel channel, String to, String subject, String body) {
}
