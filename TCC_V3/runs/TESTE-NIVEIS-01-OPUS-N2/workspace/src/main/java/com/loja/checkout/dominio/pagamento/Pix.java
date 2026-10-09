package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Sempre a vista, com 5% de desconto no total do pedido. */
@Component
public class Pix implements FormaPagamento {

    private static final BigDecimal TAXA_DESCONTO = new BigDecimal("0.05");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public Pago calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Dinheiro.percentual(totalPedido, TAXA_DESCONTO);
        BigDecimal totalFinal = Dinheiro.centavos(totalPedido.subtract(desconto));
        return new Pago(totalFinal, totalFinal);
    }
}
