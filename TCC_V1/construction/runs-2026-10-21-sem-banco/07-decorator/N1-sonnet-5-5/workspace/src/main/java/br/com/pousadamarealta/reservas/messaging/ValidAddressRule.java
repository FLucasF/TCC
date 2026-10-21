package br.com.pousadamarealta.reservas.messaging;

/** Bloqueia endereço inválido: um único @, algo antes dele e, depois, um nome com ponto no meio. */
public final class ValidAddressRule implements MessageRule {

    @Override
    public void apply(Draft draft) {
        if (!isValid(draft.to())) {
            draft.block();
        }
    }

    static boolean isValid(String address) {
        if (address == null) {
            return false;
        }
        int at = address.indexOf('@');
        if (at <= 0 || at != address.lastIndexOf('@')) {
            return false;
        }
        String domain = address.substring(at + 1);
        return domain.indexOf('.') > 0 && !domain.endsWith(".");
    }
}
