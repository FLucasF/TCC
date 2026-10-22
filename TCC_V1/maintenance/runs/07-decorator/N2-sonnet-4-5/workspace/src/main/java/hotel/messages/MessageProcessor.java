package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/** Processes messages through a chain of handlers. */
public final class MessageProcessor {

    private final List<MessageHandler> handlers;

    public MessageProcessor(List<MessageHandler> handlers) {
        this.handlers = List.copyOf(handlers);
    }

    public List<ProcessedMessage.MessageToStore> process(OutgoingMessage message) {
        OutgoingMessage current = message;
        List<ProcessedMessage.MessageToStore> additionalMessages = new ArrayList<>();
        boolean blocked = false;

        for (MessageHandler handler : handlers) {
            if (blocked) {
                break;
            }

            ProcessedMessage result = handler.handle(current);

            if (result.messages().get(0).status() == MessageStatus.BLOCKED) {
                blocked = true;
                additionalMessages.clear();
                additionalMessages.add(result.messages().get(0));
            } else {
                current = new OutgoingMessage(
                    result.messages().get(0).to(),
                    result.messages().get(0).subject(),
                    result.messages().get(0).body()
                );

                if (result.messages().size() > 1) {
                    additionalMessages.addAll(result.messages().subList(1, result.messages().size()));
                }
            }
        }

        if (blocked) {
            return additionalMessages;
        }

        List<ProcessedMessage.MessageToStore> allMessages = new ArrayList<>();
        allMessages.add(new ProcessedMessage.MessageToStore(
            current.to(), current.subject(), current.body(), MessageStatus.SENT
        ));
        allMessages.addAll(additionalMessages);
        return allMessages;
    }
}
