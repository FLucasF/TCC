package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.Codificado;

import java.math.BigDecimal;

/** Uma forma de pagamento e o ajuste que ela faz sobre o total do pedido. */
public interface FormaPagamento extends Codificado {

    default boolean permiteParcelas(int parcelas) {
        return parcelas == 1;
    }

    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    Pagamento pagar(BigDecimal totalPedido, int parcelas);
}
