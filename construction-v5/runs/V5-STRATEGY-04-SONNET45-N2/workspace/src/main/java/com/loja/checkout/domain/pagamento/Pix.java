package com.loja.checkout.domain.pagamento;

import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;

public class Pix implements FormaPagamento {
    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("0.05");

    @Override
    public boolean aceitaParcelas(int numParcelas) {
        return numParcelas == 1;
    }

    @Override
    public boolean estaDisponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Arredondamento.arredondar(totalPedido.multiply(PERCENTUAL_DESCONTO));
        BigDecimal totalFinal = totalPedido.subtract(desconto);
        BigDecimal ajuste = desconto.negate();

        return new ResultadoPagamento(ajuste, totalFinal, totalFinal);
    }
}
