package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

public class Boleto implements Pagamento {
    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE = new BigDecimal("1000.00");

    @Override public String codigo() { return "BOLETO"; }
    @Override public boolean parcelasValidas(int parcelas) { return parcelas == 1; }
    @Override public boolean disponivelPara(BigDecimal totalPedido) {
        return totalPedido.compareTo(LIMITE) <= 0;
    }
    @Override public Ajuste calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.add(TARIFA));
        return new Ajuste(TARIFA, totalFinal, totalFinal);
    }
}
