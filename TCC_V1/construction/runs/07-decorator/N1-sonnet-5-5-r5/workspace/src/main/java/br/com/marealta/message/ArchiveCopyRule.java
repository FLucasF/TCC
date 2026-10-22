package br.com.marealta.message;

/** Gera, para o arquivo do contador, uma cópia de toda mensagem enviada. */
public class ArchiveCopyRule implements MessageRule {

    private final String archiveAddress;

    public ArchiveCopyRule(String archiveAddress) {
        this.archiveAddress = archiveAddress;
    }

    @Override
    public String name() {
        return "archive-copy";
    }

    @Override
    public void apply(Delivery delivery) {
        delivery.addCopy(archiveAddress, "[Cópia] " + delivery.subject());
    }
}
