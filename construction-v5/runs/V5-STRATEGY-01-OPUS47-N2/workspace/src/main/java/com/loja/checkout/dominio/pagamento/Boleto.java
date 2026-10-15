package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

public final class Boleto implements FormaPagamento {
    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal TETO = new BigDecimal("1000.00");

    public boolean parcelasValidas(int parcelas) { return parcelas == 1; }

    public boolean atende(BigDecimal totalPedido, int parcelas) {
        return totalPedido.compareTo(TETO) <= 0;
    }

    public Pagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.add(TARIFA));
        return new Pagamento(totalFinal, totalFinal);
    }
}
