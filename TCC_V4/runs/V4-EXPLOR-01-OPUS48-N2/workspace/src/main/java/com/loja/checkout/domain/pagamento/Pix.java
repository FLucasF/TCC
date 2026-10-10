package com.loja.checkout.domain.pagamento;

import com.loja.checkout.domain.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** À vista, com 5% de desconto sobre o total do pedido. */
@Component
public class Pix implements FormaPagamento {

    private static final BigDecimal DESCONTO = new BigDecimal("0.05");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public PagamentoResultado calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Dinheiro.centavos(totalPedido.multiply(DESCONTO));
        BigDecimal totalFinal = Dinheiro.centavos(totalPedido.subtract(desconto));
        return new PagamentoResultado(totalFinal, totalFinal);
    }
}
