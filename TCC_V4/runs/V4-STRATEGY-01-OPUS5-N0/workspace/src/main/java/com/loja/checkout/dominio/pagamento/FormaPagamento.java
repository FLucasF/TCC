package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Codificavel;
import java.math.BigDecimal;

/**
 * Uma forma de pagar o pedido.
 *
 * <p>Forma nova e uma classe anotada com {@code @Component} implementando esta interface.
 */
public interface FormaPagamento extends Codificavel {

    /** {@code false} quando o numero de parcelas nao e permitido nesta forma de pagamento. */
    boolean parcelamentoPermitido(int parcelas);

    /** {@code false} quando esta forma nao atende o pedido (ex.: boleto acima de R$ 1.000,00). */
    default boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    /** Valor final e parcela, a partir do total do pedido. */
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
