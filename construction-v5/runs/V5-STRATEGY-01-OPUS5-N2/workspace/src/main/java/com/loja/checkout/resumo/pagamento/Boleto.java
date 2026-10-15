package com.loja.checkout.resumo.pagamento;

import com.loja.checkout.resumo.Dinheiro;
import java.math.BigDecimal;

/** À vista, com a tarifa do banco somada ao total, só até R$ 1.000,00 de pedido. */
public class Boleto implements FormaPagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000.00");

    @Override
    public Parcelamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = Dinheiro.centavos(totalPedido.add(TARIFA));
        return new Parcelamento(totalFinal, totalFinal);
    }

    @Override
    public boolean permiteParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return totalPedido.compareTo(TOTAL_MAXIMO) <= 0;
    }
}
