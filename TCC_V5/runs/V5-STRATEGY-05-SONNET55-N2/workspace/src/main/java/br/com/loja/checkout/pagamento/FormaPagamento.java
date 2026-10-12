package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.Identificavel;
import java.math.BigDecimal;

public interface FormaPagamento extends Identificavel {

    default boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    default boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);
}
