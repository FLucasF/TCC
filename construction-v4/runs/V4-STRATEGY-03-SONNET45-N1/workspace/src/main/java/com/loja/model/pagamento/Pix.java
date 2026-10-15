package com.loja.model.pagamento;

import com.loja.util.Dinheiro;

import java.math.BigDecimal;

public class Pix implements FormaPagamento {
    @Override
    public BigDecimal calcularTotalFinal(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Dinheiro.arredondar(totalPedido.multiply(new BigDecimal("0.05")));
        return totalPedido.subtract(desconto);
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean aceitaPedido(BigDecimal totalPedido) {
        return true;
    }
}
