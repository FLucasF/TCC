package com.loja.checkout.service.pagamento;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PagamentoPix implements EstrategiaPagamento {

    @Override
    public String getCodigo() {
        return "PIX";
    }

    @Override
    public boolean aceitaParcelamento(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean atendePedido(BigDecimal total) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
        // desconto = 5% de total (arredondado HALF_EVEN)
        BigDecimal desconto = total.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = total.subtract(desconto).setScale(2, RoundingMode.HALF_EVEN);
        return new ResultadoPagamento(totalFinal, 1, totalFinal);
    }
}
