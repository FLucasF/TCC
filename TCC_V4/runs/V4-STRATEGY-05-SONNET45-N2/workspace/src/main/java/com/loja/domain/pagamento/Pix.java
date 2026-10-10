package com.loja.domain.pagamento;

import com.loja.util.Moeda;
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
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Moeda.arredondar(totalPedido.multiply(new BigDecimal("0.05")));
        BigDecimal totalFinal = Moeda.arredondar(totalPedido.subtract(desconto));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        return new ResultadoPagamento(ajuste, totalFinal, totalFinal);
    }
}
