package com.loja.checkout.dominio;

/** Traduz o codigo que o site envia na opcao correspondente, ou recusa o pedido. */
public final class Enums {

    private Enums() {
    }

    public static <E extends Enum<E>> E obrigatorio(Class<E> tipo, String codigo, CodigoErro erro) {
        if (codigo == null) {
            throw new PedidoRecusadoException(erro);
        }
        try {
            return Enum.valueOf(tipo, codigo);
        } catch (IllegalArgumentException naoExiste) {
            throw new PedidoRecusadoException(erro);
        }
    }
}
