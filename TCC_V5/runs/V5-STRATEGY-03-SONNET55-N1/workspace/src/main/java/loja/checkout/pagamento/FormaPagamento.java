package loja.checkout.pagamento;

import java.math.BigDecimal;
import loja.checkout.dominio.Identificavel;

public interface FormaPagamento extends Identificavel {

    boolean aceitaParcelas(int parcelas);

    default boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);
}
