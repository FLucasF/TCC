package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.comum.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** 5% de desconto no total do pedido, sempre a vista. */
@Component
public class PagamentoPix implements FormaPagamento {

    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("5");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Dinheiro.percentual(totalPedido, PERCENTUAL_DESCONTO);
        BigDecimal totalFinal = Dinheiro.valor(totalPedido.subtract(desconto));
        return new ResultadoPagamento(totalFinal, totalFinal);
    }
}
