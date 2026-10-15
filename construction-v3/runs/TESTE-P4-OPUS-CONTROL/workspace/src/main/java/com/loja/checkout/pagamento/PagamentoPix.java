package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** 5% de desconto no total do pedido, sempre a vista. */
@Component
public class PagamentoPix implements FormaPagamento {

    private static final BigDecimal DESCONTO = new BigDecimal("0.05");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public ResultadoPagamento calcular(ContextoPagamento contexto) {
        BigDecimal total = contexto.totalPedido();
        BigDecimal totalFinal = Dinheiro.centavos(total.subtract(Dinheiro.percentual(total, DESCONTO)));
        return new ResultadoPagamento(totalFinal, totalFinal);
    }
}
