package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.pedido.Opcao;
import java.math.BigDecimal;

/** Uma forma de pagamento: parcelamentos aceitos, pedidos que atende e quanto o cliente paga. */
public interface FormaPagamento extends Opcao {

    boolean aceitaParcelas(int parcelas);

    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    Pagamento pagar(BigDecimal totalPedido, int parcelas);
}
