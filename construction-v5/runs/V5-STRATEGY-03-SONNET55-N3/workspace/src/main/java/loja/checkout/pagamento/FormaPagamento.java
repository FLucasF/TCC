package loja.checkout.pagamento;

import java.math.BigDecimal;
import loja.checkout.Codificado;

public interface FormaPagamento extends Codificado {
    boolean parcelasPermitidas(int parcelas);

    boolean disponivel(BigDecimal totalPedido);

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);
}
