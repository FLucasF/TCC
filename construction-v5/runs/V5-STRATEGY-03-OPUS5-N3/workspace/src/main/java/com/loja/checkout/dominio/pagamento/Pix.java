package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** 5% de desconto no total do pedido, sempre à vista. */
@Component
public class Pix extends AVista {

    private static final BigDecimal DESCONTO = new BigDecimal("0.05");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = Dinheiro.arredonda(totalPedido.subtract(Dinheiro.percentual(totalPedido, DESCONTO)));
        return new ResultadoPagamento(totalFinal, totalFinal);
    }
}
