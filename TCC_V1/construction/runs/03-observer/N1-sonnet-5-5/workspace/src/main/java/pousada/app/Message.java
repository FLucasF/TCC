package pousada.app;

public record Message(long id, Channel channel, String to, String subject, String body) {
    public enum Channel { EMAIL }
}
