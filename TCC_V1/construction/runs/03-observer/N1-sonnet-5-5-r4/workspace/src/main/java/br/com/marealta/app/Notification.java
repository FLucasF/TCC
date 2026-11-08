package br.com.marealta.app;

/** Uma mensagem a enviar, ainda sem número. */
public record Notification(Channel channel, String to, String subject, String body) {
    public static Notification email(String to, String subject, String body) {
        return new Notification(Channel.EMAIL, to, subject, body);
    }
}
