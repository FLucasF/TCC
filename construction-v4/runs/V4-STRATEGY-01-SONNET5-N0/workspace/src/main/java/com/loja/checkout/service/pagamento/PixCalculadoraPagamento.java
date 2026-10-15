package com.loja.checkout.service.pagamento;

import com.loja.checkout.enums.FormaPagamento;
import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PixCalculadoraPagamento implements CalculadoraPagamento {

    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("0.05");

    @Override
    public FormaPagamento getForma() {
        return FormaPagamento.PIX;
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
        BigDecimal desconto = Dinheiro.arredondar(totalPedido.multiply(PERCENTUAL_DESCONTO));
        BigDecimal ajuste = desconto.negate();
        BigDecimal totalFinal = totalPedido.add(ajuste);
        return new ResultadoPagamento(ajuste, totalFinal, totalFinal);
    }
}
