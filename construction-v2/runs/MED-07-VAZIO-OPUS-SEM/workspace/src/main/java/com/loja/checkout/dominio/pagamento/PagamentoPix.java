package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Pix: sempre a vista, com 5% de desconto no total do pedido. */
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
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Dinheiro.arredondar(totalPedido.multiply(DESCONTO));
        BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.subtract(desconto));
        return new ResultadoPagamento(totalFinal, 1, totalFinal);
    }
}
