package com.loja.checkout.dominio.pagamento;

/** Uma forma de pagamento aceita pela loja. */
public interface FormaPagamento {

    String codigo();

    /** Se o numero de parcelas escolhido e permitido nessa forma de pagamento. */
    boolean permiteParcelas(int parcelas);

    /** Se a forma de pagamento atende esse pedido. */
    default boolean disponivel(ContextoPagamento contexto) {
        return true;
    }

    /** Total final e valor da parcela depois do ajuste. */
    ResultadoPagamento liquidar(ContextoPagamento contexto);
}
