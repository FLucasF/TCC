package com.loja.checkout.domain.pagamento;

import com.loja.checkout.domain.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Pix: sempre a vista, 5% de desconto sobre o total do pedido. */
@Component
public class PagamentoPix implements FormaPagamento {

    private static final BigDecimal DESCONTO = new BigDecimal("0.05");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean parcelamentoValido(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Dinheiro.centavos(totalPedido.multiply(DESCONTO));
        BigDecimal totalFinal = totalPedido.subtract(desconto);
        return new ResultadoPagamento(1, totalFinal, totalFinal, desconto.negate());
    }
}
