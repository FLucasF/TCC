package com.loja.checkout.pagamento;

import com.loja.checkout.Identificavel;
import java.math.BigDecimal;

/** Uma forma de pagamento: parcelamento aceito, disponibilidade e valor final cobrado. */
public interface Pagamento extends Identificavel {
    boolean aceitaParcelas(int parcelas);

    boolean disponivel(BigDecimal totalPedido);

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);
}
