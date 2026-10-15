package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Pix: sempre à vista, com 5% de desconto sobre o total do pedido.
 */
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
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Dinheiro.centavos(totalPedido.multiply(DESCONTO));
        BigDecimal totalFinal = Dinheiro.centavos(totalPedido.subtract(desconto));
        return new ResultadoPagamento(totalFinal, totalFinal);
    }
}
