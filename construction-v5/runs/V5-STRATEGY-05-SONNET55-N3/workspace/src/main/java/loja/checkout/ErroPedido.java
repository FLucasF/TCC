package loja.checkout;

public class ErroPedido extends RuntimeException {
    private final String codigo;

    public ErroPedido(String codigo) {
        super(codigo, null, false, false);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
