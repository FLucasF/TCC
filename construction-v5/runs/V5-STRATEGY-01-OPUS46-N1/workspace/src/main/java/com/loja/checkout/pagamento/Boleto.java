package com.loja.checkout.pagamento;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Boleto implements FormaPagamento {

    @Override
    public String codigo() {
        return "BOLETO";
    }

    @Override
    public boolean parcelamentoValido(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido, int parcelas) {
        return totalPedido.compareTo(new BigDecimal("1000.00")) <= 0;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = totalPedido.add(new BigDecimal("3.49"));
        return new ResultadoPagamento(totalFinal, totalFinal, 1);
    }
}
