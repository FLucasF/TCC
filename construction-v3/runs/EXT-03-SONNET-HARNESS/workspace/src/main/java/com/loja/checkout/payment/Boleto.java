package com.loja.checkout.payment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Boleto implements PaymentMethod {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE = new BigDecimal("1000.00");

    @Override
    public String getCodigo() {
        return "BOLETO";
    }

    @Override
    public boolean isParcelasValidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean isDisponivel(BigDecimal totalProdutosComCupomEFrete) {
        return totalProdutosComCupomEFrete.compareTo(LIMITE) <= 0;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = totalPedido.add(TARIFA);
        return new ResultadoPagamento(totalFinal, totalFinal);
    }
}
