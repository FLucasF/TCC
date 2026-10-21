package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageStatus;

import java.util.ArrayList;
import java.util.List;

public class ArchiveCopyRule implements MessageRule {
    private final String archiveEmail;
    private long nextCopyId;

    public ArchiveCopyRule(String archiveEmail, long nextCopyId) {
        this.archiveEmail = archiveEmail;
        this.nextCopyId = nextCopyId;
    }

    @Override
    public List<Message> process(List<Message> messages) {
        List<Message> result = new ArrayList<>();
        for (Message message : messages) {
            result.add(message);
            if (message.status() == MessageStatus.SENT) {
                result.add(createArchiveCopy(message));
            }
        }
        return result;
    }

    private Message createArchiveCopy(Message original) {
        return new Message(
            nextCopyId++,
            original.channel(),
            archiveEmail,
            "[Cópia] " + original.subject(),
            original.body(),
            MessageStatus.SENT
        );
    }
}
