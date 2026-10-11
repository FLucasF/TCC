package com.loja.checkout.pagamento;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PagamentoPix implements CalculoPagamento {

    private static final BigDecimal DESCONTO = new BigDecimal("0.05");

    @Override
    public String getCodigo() {
        return "PIX";
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal valorDesconto = totalPedido.multiply(DESCONTO).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = totalPedido.subtract(valorDesconto);
        return new ResultadoPagamento(totalFinal, totalFinal, 1);
    }

    @Override
    public boolean isParcelamentoValido(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean isDisponivel(BigDecimal totalPedido) {
        return true;
    }
}
