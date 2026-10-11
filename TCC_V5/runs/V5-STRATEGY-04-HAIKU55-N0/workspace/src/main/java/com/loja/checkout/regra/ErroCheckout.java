package com.loja.checkout.regra;

public class ErroCheckout extends RuntimeException {

    private final CodigoErro codigo;

    public ErroCheckout(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoErro getCodigo() {
        return codigo;
    }

    static <E extends Enum<E>> E paraEnum(Class<E> tipo, String valor, CodigoErro erroSeInvalido) {
        if (valor == null) {
            throw new ErroCheckout(erroSeInvalido);
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            throw new ErroCheckout(erroSeInvalido);
        }
    }
}
