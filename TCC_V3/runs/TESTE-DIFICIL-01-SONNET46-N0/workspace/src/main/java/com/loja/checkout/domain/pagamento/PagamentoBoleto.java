package com.loja.checkout.domain.pagamento;

import com.loja.checkout.domain.clube.BeneficioClube;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PagamentoBoleto implements FormaPagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_TOTAL = new BigDecimal("1000.00");

    @Override
    public String getCodigo() {
        return "BOLETO";
    }

    @Override
    public boolean isParcelasValida(int parcelas, BeneficioClube nivel) {
        return parcelas == 1;
    }

    @Override
    public boolean isDisponivel(BigDecimal totalPedido) {
        return totalPedido.compareTo(LIMITE_TOTAL) <= 0;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas, BeneficioClube nivel) {
        BigDecimal totalFinal = totalPedido.add(TARIFA).setScale(2, RoundingMode.HALF_EVEN);
        return new ResultadoPagamento(TARIFA, totalFinal, totalFinal, 2);
    }
}
