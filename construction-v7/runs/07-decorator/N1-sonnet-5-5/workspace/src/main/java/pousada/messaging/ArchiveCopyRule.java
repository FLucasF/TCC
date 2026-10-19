package pousada.messaging;

import java.util.List;

/** Fora do modo de teste, cada mensagem enviada gera uma cópia para o arquivo, logo depois dela. */
public class ArchiveCopyRule implements MessageRule {
    private final String archiveAddress;

    public ArchiveCopyRule(String archiveAddress) {
        this.archiveAddress = archiveAddress;
    }

    @Override
    public RuleScope scope() {
        return RuleScope.LIVE_ONLY;
    }

    @Override
    public List<Message> apply(Message message) {
        Message copy = message.withTo(archiveAddress).withSubject("[Cópia] " + message.subject());
        return List.of(message, copy);
    }
}
