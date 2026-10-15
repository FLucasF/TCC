package br.com.loja.checkout.dominio;

public class ErroPedidoException extends RuntimeException {

    private final CodigoErro codigo;

    public ErroPedidoException(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoErro getCodigo() {
        return codigo;
    }
}
