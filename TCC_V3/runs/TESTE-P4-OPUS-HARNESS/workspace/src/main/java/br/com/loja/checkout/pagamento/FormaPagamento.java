package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.dominio.Codificado;
import java.math.BigDecimal;

/**
 * Uma forma de pagamento. Cada forma tem seu ajuste sobre o total do pedido,
 * seu parcelamento permitido e suas limitacoes.
 */
public interface FormaPagamento extends Codificado {

    boolean permiteParcelas(int parcelas);

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);

    /** Se esta forma atende o pedido, pelo total antes do imposto (produtos - cupom + frete). */
    default boolean atende(BigDecimal totalAntesDoImposto) {
        return true;
    }
}
