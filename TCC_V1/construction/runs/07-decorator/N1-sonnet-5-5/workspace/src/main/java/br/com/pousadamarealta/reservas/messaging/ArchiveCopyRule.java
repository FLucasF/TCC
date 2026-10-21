package br.com.pousadamarealta.reservas.messaging;

/** Toda mensagem enviada gera uma cópia para o arquivo do contador. */
public final class ArchiveCopyRule implements MessageRule {

    private final String archiveAddress;

    public ArchiveCopyRule(String archiveAddress) {
        this.archiveAddress = archiveAddress;
    }

    @Override
    public void apply(Draft draft) {
        draft.addCopy(archiveAddress, "[Cópia] " + draft.subject(), draft.body());
    }
}
