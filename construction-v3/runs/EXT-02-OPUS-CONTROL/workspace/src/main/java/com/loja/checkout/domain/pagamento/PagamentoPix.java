package com.loja.checkout.domain.pagamento;

import com.loja.checkout.domain.pedido.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Pix: 5% de desconto no total do pedido, sempre a vista. */
@Component
public class PagamentoPix implements FormaPagamento {

    private static final BigDecimal DESCONTO = new BigDecimal("0.05");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public ResultadoPagamento aplicar(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = Dinheiro.valor(totalPedido.subtract(Dinheiro.percentual(totalPedido, DESCONTO)));
        return new ResultadoPagamento(totalFinal, totalFinal);
    }
}
