package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.Codificado;
import java.math.BigDecimal;

public interface FormaPagamento extends Codificado {

    boolean aceitaParcelas(int parcelas);

    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);
}
