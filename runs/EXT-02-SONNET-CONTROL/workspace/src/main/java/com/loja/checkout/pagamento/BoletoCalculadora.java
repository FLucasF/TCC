package com.loja.checkout.pagamento;

import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BoletoCalculadora implements CalculadoraPagamento {

    private static final BigDecimal LIMITE_TOTAL_PEDIDO = new BigDecimal("1000.00");
    private static final BigDecimal TARIFA = new BigDecimal("3.49");

    @Override
    public FormaPagamento formaPagamento() {
        return FormaPagamento.BOLETO;
    }

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean disponivelPara(BigDecimal totalPedido) {
        return totalPedido.compareTo(LIMITE_TOTAL_PEDIDO) <= 0;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = Money.round(totalPedido.add(TARIFA));
        return new ResultadoPagamento(TARIFA, totalFinal, totalFinal);
    }
}
