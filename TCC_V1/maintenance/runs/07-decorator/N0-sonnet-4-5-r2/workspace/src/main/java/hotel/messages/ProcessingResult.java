package hotel.messages;

import java.util.ArrayList;
import java.util.List;

public final class ProcessingResult {

    private OutgoingMessage original;
    private final List<OutgoingMessage> additional;

    public ProcessingResult(OutgoingMessage original) {
        this.original = original;
        this.additional = new ArrayList<>();
    }

    public OutgoingMessage original() {
        return original;
    }

    public void setOriginal(OutgoingMessage message) {
        this.original = message;
    }

    public List<OutgoingMessage> additional() {
        return additional;
    }

    public void addMessage(OutgoingMessage message) {
        additional.add(message);
    }
}
