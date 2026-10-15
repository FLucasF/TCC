package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

class Boleto implements FormaPagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE = new BigDecimal("1000.00");

    @Override
    public String codigo() { return "BOLETO"; }

    @Override
    public boolean aceitaParcelas(int parcelas) { return parcelas == 1; }

    @Override
    public boolean disponivel(BigDecimal total) {
        return total.compareTo(LIMITE) <= 0;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
        BigDecimal totalFinal = total.add(TARIFA);
        return new ResultadoPagamento(TARIFA, totalFinal, totalFinal);
    }
}
