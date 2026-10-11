package com.loja.checkout.pagamento;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class Boleto implements FormaPagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE = new BigDecimal("1000.00");

    @Override
    public String getCodigo() {
        return "BOLETO";
    }

    @Override
    public boolean isParcelamentoValido(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean isDisponivel(BigDecimal totalPedido) {
        return totalPedido.compareTo(LIMITE) <= 0;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = totalPedido.add(TARIFA);
        return new ResultadoPagamento(totalFinal, totalFinal, 1);
    }
}
