package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.comum.Codificado;
import java.math.BigDecimal;

/**
 * Uma forma de pagamento: em quantas vezes aceita, quais pedidos aceita e
 * como ela mexe no total do pedido.
 */
public interface FormaPagamento extends Codificado {

    boolean permiteParcelas(int parcelas);

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);

    default boolean atende(BigDecimal totalPedido) {
        return true;
    }
}
