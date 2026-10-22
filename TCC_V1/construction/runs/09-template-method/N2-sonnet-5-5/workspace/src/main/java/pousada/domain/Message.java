package pousada.domain;

public record Message(Long id, Channel channel, String to, String subject, String body) {
    public enum Channel { EMAIL }
}
