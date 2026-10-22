package br.com.marealta.pousada.mail.rules;

import br.com.marealta.pousada.mail.Message;
import br.com.marealta.pousada.mail.MessageRule;
import java.util.List;

/** Gera uma cópia para o arquivo do contador, logo depois da mensagem original. */
public class ArchiveCopyRule implements MessageRule {
    private final String archiveAddress;

    public ArchiveCopyRule(String archiveAddress) {
        this.archiveAddress = archiveAddress;
    }

    @Override
    public List<Message> apply(Message message) {
        Message copy = Message.email(archiveAddress, "[Cópia] " + message.subject(), message.body());
        return List.of(message, copy);
    }
}
