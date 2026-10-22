package hotel.messages;

@FunctionalInterface
public interface EmailRule {
    void apply(ProcessingResult result);
}
