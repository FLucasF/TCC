package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** 5% de desconto no total do pedido. */
@Component
public class Pix implements PagamentoAVista {

    private static final BigDecimal TAXA_DESCONTO = new BigDecimal("0.05");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public BigDecimal totalFinal(BigDecimal totalPedido) {
        return Dinheiro.arredonda(totalPedido.subtract(Dinheiro.percentual(totalPedido, TAXA_DESCONTO)));
    }
}
