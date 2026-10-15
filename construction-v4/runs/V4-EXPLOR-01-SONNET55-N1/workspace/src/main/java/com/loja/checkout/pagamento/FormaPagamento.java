package com.loja.checkout.pagamento;

import com.loja.checkout.Codificado;
import java.math.BigDecimal;

public interface FormaPagamento extends Codificado {
    boolean aceitaParcelas(int parcelas);

    boolean disponivel(BigDecimal totalPedido);

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);
}
