package br.com.marealta.mail;

/** Mensagem pronta para ser guardada. guestAddress é o endereço original do hóspede (null nas cópias). */
public record Outgoing(String to, String subject, String body, String status, String guestAddress) {
    public static final String SENT = "SENT";
    public static final String BLOCKED = "BLOCKED";
}
