package com.loja.checkout.pagamento;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class BoletoPagamento implements FormaPagamento {

    private static final BigDecimal LIMITE_TOTAL = new BigDecimal("1000.00");
    private static final BigDecimal TARIFA = new BigDecimal("3.49");

    @Override
    public String codigo() {
        return "BOLETO";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return totalPedido.compareTo(LIMITE_TOTAL) <= 0;
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        BigDecimal valorFinal = totalPedido.add(TARIFA);
        return new Cobranca(valorFinal, valorFinal);
    }
}
