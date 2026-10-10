package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

/**
 * Boleto: sempre a vista, com a tarifa do banco somada ao total, e nao e
 * aceito quando o total do pedido passa de R$ 1.000,00.
 */
public final class Boleto implements FormaPagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000.00");

    @Override
    public String codigo() {
        return "BOLETO";
    }

    @Override
    public boolean permiteParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return totalPedido.compareTo(TOTAL_MAXIMO) <= 0;
    }

    @Override
    public Liquidacao liquidar(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = Dinheiro.centavos(totalPedido.add(TARIFA));
        return new Liquidacao(totalFinal, totalFinal);
    }
}
