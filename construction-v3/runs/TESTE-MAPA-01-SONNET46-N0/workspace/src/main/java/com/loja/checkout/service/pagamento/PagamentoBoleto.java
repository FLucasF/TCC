package com.loja.checkout.service.pagamento;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PagamentoBoleto implements EstrategiaPagamento {

    private static final BigDecimal TAXA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE = new BigDecimal("1000.00");

    @Override
    public String getCodigo() {
        return "BOLETO";
    }

    @Override
    public boolean aceitaParcelamento(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean atendePedido(BigDecimal total) {
        return total.compareTo(LIMITE) <= 0;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
        BigDecimal totalFinal = total.add(TAXA).setScale(2, RoundingMode.HALF_EVEN);
        return new ResultadoPagamento(totalFinal, 1, totalFinal);
    }
}
