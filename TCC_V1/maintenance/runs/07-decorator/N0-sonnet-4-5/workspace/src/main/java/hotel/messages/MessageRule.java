package hotel.messages;

import java.util.List;

public interface MessageRule {
    List<OutgoingMessage> apply(OutgoingMessage message);
}
