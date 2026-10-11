package com.loja.checkout.pagamento;

import com.loja.checkout.Identificavel;
import java.math.BigDecimal;

public interface FormaPagamento extends Identificavel {
    boolean aceitaParcelas(int parcelas);

    boolean disponivel(BigDecimal totalPedido);

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);
}
