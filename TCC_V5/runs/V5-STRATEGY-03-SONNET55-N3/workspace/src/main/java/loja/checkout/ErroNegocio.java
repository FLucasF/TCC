package loja.checkout;

public class ErroNegocio extends RuntimeException {
    private final String codigo;

    public ErroNegocio(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
