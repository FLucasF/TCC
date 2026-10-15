package com.loja.checkout.domain.pagamento;

import com.loja.checkout.domain.clube.BeneficioClube;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PagamentoPix implements FormaPagamento {

    @Override
    public String getCodigo() {
        return "PIX";
    }

    @Override
    public boolean isParcelasValida(int parcelas, BeneficioClube nivel) {
        return parcelas == 1;
    }

    @Override
    public boolean isDisponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas, BeneficioClube nivel) {
        BigDecimal desconto = totalPedido.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = totalPedido.subtract(desconto);
        return new ResultadoPagamento(desconto.negate(), totalFinal, totalFinal, 0);
    }
}
