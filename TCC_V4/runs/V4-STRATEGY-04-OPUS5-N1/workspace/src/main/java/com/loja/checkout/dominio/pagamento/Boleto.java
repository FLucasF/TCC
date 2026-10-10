package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Centavos;
import java.math.BigDecimal;

/** Sempre a vista, soma a tarifa do banco e nao atende pedidos acima de R$ 1.000,00. */
public final class Boleto implements FormaPagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000.00");

    @Override
    public String codigo() {
        return "BOLETO";
    }

    @Override
    public boolean parcelamentoPermitido(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return totalPedido.compareTo(TOTAL_MAXIMO) <= 0;
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = Centavos.arredondar(totalPedido.add(TARIFA));
        return new Cobranca(totalFinal, totalFinal);
    }
}
