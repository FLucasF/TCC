package br.com.marealta.pousada.domain;

public record Message(int id, Channel channel, String to, String subject, String body) {
    public enum Channel { EMAIL }
}
