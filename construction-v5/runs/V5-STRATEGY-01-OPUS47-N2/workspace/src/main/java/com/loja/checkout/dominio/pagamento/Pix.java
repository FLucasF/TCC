package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

public final class Pix implements FormaPagamento {
    private static final BigDecimal DESCONTO = new BigDecimal("0.05");

    public boolean parcelasValidas(int parcelas) { return parcelas == 1; }

    public boolean atende(BigDecimal totalPedido, int parcelas) { return true; }

    public Pagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Dinheiro.arredondar(totalPedido.multiply(DESCONTO));
        BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.subtract(desconto));
        return new Pagamento(totalFinal, totalFinal);
    }
}
