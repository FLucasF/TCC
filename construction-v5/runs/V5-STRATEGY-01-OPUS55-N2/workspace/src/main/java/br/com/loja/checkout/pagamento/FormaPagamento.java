package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.resumo.Codificado;
import java.math.BigDecimal;

/** Cada forma de pagamento decide parcelamento, disponibilidade e o valor final sobre o total do pedido. */
public interface FormaPagamento extends Codificado {

    boolean aceitaParcelas(int parcelas);

    boolean atende(BigDecimal totalPedido);

    Pagamento pagar(BigDecimal totalPedido, int parcelas);
}
