package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Centavos;
import java.math.BigDecimal;

/** Sempre a vista, com 5% de desconto no total do pedido. */
public final class Pix implements FormaPagamento {

    private static final BigDecimal TAXA_DESCONTO = new BigDecimal("0.05");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean parcelamentoPermitido(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Centavos.percentual(totalPedido, TAXA_DESCONTO);
        BigDecimal totalFinal = Centavos.arredondar(totalPedido.subtract(desconto));
        return new Cobranca(totalFinal, totalFinal);
    }
}
