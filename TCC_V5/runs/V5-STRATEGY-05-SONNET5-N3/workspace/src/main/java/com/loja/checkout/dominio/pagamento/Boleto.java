package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Boleto implements CalculoPagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_TOTAL = new BigDecimal("1000.00");

    @Override
    public FormaPagamento forma() {
        return FormaPagamento.BOLETO;
    }

    @Override
    public boolean parcelaPermitida(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return totalPedido.compareTo(LIMITE_TOTAL) <= 0;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = totalPedido.add(TARIFA);
        return new ResultadoPagamento(TARIFA, totalFinal, totalFinal);
    }
}
