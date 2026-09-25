package com.loja.checkout.pagamento.impl;

import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.util.Arredondamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PixPagamento implements FormaPagamento {

    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("0.05");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean disponivelPara(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Arredondamento.paraCentavos(totalPedido.multiply(PERCENTUAL_DESCONTO));
        BigDecimal ajuste = desconto.negate();
        BigDecimal totalFinal = totalPedido.add(ajuste);
        return new ResultadoPagamento(ajuste, totalFinal, 1, totalFinal);
    }
}
