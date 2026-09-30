package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.comum.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Sempre a vista, com 5% de desconto no total do pedido. */
@Component
public class PagamentoPix implements FormaPagamento {

    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("5");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public ResultadoPagamento calcular(ContextoPagamento contexto, int parcelas) {
        BigDecimal desconto = Dinheiro.percentual(contexto.totalPedido(), PERCENTUAL_DESCONTO);
        BigDecimal totalFinal = Dinheiro.centavos(contexto.totalPedido().subtract(desconto));
        return new ResultadoPagamento(totalFinal, totalFinal);
    }
}
