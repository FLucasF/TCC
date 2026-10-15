package com.loja.checkout.domain.pagamento;

import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.util.Dinheiro;

import java.math.BigDecimal;

public class Pix implements FormaPagamento {
    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = totalPedido.multiply(new BigDecimal("0.05"));
        return Dinheiro.arredondar(desconto).negate();
    }

    @Override
    public int numeroParcelas(int parcelasDesejadas) {
        return 1;
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalFinal, int parcelas) {
        return totalFinal;
    }

    @Override
    public boolean aceitaPedido(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public boolean parcelamentoValido(int parcelas) {
        return parcelas == 1;
    }
}
