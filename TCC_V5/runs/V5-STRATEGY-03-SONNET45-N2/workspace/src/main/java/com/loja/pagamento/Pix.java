package com.loja.pagamento;

import com.loja.util.Arredondador;
import java.math.BigDecimal;

public class Pix implements FormaPagamento {

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean aceita(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        BigDecimal percentual = new BigDecimal("0.05");
        BigDecimal desconto = Arredondador.arredondar(totalPedido.multiply(percentual));
        return desconto.negate();
    }

    @Override
    public int numeroParcelas(int parcelasInformadas) {
        return 1;
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalFinal, int parcelas) {
        return totalFinal;
    }
}
