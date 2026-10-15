package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Pix: sempre à vista, com 5% de desconto no total do pedido. */
@Component
public class Pix implements FormaPagamento {

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
    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Dinheiro.porcentagem(totalPedido, PERCENTUAL_DESCONTO);
        BigDecimal totalFinal = Dinheiro.centavos(totalPedido.subtract(desconto));
        BigDecimal ajuste = Dinheiro.centavos(totalFinal.subtract(totalPedido));
        return new ResultadoPagamento(1, totalFinal, totalFinal, ajuste);
    }
}
