package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** A vista, com 5% de desconto no total do pedido. */
@Component
public class Pix implements FormaPagamento {

    private static final BigDecimal TAXA_DESCONTO = new BigDecimal("0.05");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Dinheiro.percentual(totalPedido, TAXA_DESCONTO);
        BigDecimal totalFinal = Dinheiro.centavos(totalPedido.subtract(desconto));
        return new Cobranca(totalFinal, 1, totalFinal);
    }

    @Override
    public boolean permiteParcelas(int parcelas) {
        return parcelas == 1;
    }
}
