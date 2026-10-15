package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Opcao;
import java.math.BigDecimal;

public interface FormaPagamento extends Opcao {

    boolean aceitaParcelas(int parcelas);

    boolean disponivel(BigDecimal totalPedido);

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);
}
