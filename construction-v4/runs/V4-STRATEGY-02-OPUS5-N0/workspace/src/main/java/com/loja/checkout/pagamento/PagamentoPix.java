package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Pix: 5% de desconto no total do pedido, sempre à vista. */
@Component
public class PagamentoPix implements FormaPagamento {

    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("5");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Dinheiro.percentual(totalPedido, PERCENTUAL_DESCONTO);
        BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.subtract(desconto));
        return new ResultadoPagamento(totalFinal, totalFinal);
    }
}
