package com.loja.checkout.domain.pagamento;

import com.loja.checkout.service.ArredondamentoUtil;
import java.math.BigDecimal;

public class Pix implements FormaPagamento {

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = totalPedido.multiply(new BigDecimal("0.05"));
        return ArredondamentoUtil.arredondar(desconto).negate();
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, BigDecimal ajuste, int parcelas) {
        return totalPedido.add(ajuste);
    }

    @Override
    public boolean aceitaParcelamento(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean aceitaPedido(BigDecimal totalPedido) {
        return true;
    }
}
