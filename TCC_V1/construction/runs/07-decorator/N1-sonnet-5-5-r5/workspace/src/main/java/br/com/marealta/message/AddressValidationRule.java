package br.com.marealta.message;

/** Bloqueia endereço sem um único @, sem nada antes dele ou sem um nome com ponto no meio depois. */
public class AddressValidationRule implements MessageRule {

    @Override
    public String name() {
        return "address-validation";
    }

    @Override
    public void apply(Delivery delivery) {
        if (!isValid(delivery.to())) {
            delivery.block();
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
        return domain.indexOf('.', 1) > 0 && !domain.endsWith(".") && !domain.startsWith(".");
    }
}
