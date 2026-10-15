package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Moeda;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Pix: 5% de desconto no total do pedido, sempre a vista. */
@Component
public class Pix implements FormaPagamento {

    private static final BigDecimal DESCONTO = new BigDecimal("0.05");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean permiteParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public ResultadoPagamento liquidar(ContextoPagamento contexto) {
        BigDecimal desconto = Moeda.percentual(contexto.totalPedido(), DESCONTO);
        BigDecimal totalFinal = Moeda.centavos(contexto.totalPedido().subtract(desconto));
        return new ResultadoPagamento(totalFinal, totalFinal);
    }
}
