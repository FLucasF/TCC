package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.calculo.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** À vista, com 5% de desconto sobre o total do pedido. */
@Component
public class PagamentoPix implements FormaPagamento {

    private static final BigDecimal PORCENTAGEM_DESCONTO = new BigDecimal("5");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = totalPedido.subtract(Dinheiro.percentual(totalPedido, PORCENTAGEM_DESCONTO));
        return new ResultadoPagamento(totalFinal, 1, totalFinal);
    }
}
