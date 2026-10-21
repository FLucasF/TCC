package br.com.pousadamarealta;

public record Message(long id, Channel channel, String to, String subject, String body, Status status) {
    public enum Channel { EMAIL }

    public enum Status { SENT, BLOCKED }
}
