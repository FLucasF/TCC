package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Pix: 5% de desconto no total do pedido, sempre a vista. */
@Component
public class PagamentoPix implements FormaPagamento {

    private static final BigDecimal PERCENTUAL_DESCONTO = Dinheiro.de("0.05");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Dinheiro.arredondar(totalPedido.multiply(PERCENTUAL_DESCONTO));
        BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.subtract(desconto));
        return new ResultadoPagamento(totalFinal, totalFinal);
    }
}
