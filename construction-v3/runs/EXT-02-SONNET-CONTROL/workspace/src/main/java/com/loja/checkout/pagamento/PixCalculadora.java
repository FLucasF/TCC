package com.loja.checkout.pagamento;

import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PixCalculadora implements CalculadoraPagamento {

    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("0.05");

    @Override
    public FormaPagamento formaPagamento() {
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
        BigDecimal desconto = Money.round(totalPedido.multiply(PERCENTUAL_DESCONTO));
        BigDecimal totalFinal = Money.round(totalPedido.subtract(desconto));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        return new ResultadoPagamento(ajuste, totalFinal, totalFinal);
    }
}
