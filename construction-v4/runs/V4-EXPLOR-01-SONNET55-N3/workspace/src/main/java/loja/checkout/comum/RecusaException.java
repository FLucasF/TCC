package loja.checkout.comum;

public class RecusaException extends RuntimeException {
    private final String codigo;

    public RecusaException(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
