package loja.checkout.pagamento;

import java.math.BigDecimal;
import loja.checkout.Codificado;

public interface FormaPagamento extends Codificado {
    boolean aceitaParcelas(int parcelas);

    boolean atende(BigDecimal totalPedido);

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);
}
