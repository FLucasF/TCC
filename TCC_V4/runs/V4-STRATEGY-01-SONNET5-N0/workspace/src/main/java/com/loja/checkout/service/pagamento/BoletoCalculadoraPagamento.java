package com.loja.checkout.service.pagamento;

import com.loja.checkout.enums.FormaPagamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BoletoCalculadoraPagamento implements CalculadoraPagamento {

    private static final BigDecimal TARIFA_BANCO = new BigDecimal("3.49");
    private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000.00");

    @Override
    public FormaPagamento getForma() {
        return FormaPagamento.BOLETO;
    }

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean disponivelPara(BigDecimal totalPedido) {
        return totalPedido.compareTo(TOTAL_MAXIMO) <= 0;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal ajuste = TARIFA_BANCO;
        BigDecimal totalFinal = totalPedido.add(ajuste);
        return new ResultadoPagamento(ajuste, totalFinal, totalFinal);
    }
}
