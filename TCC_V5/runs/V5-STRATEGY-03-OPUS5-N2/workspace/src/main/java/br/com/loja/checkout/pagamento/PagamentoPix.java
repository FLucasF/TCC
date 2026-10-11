package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class PagamentoPix implements PagamentoAVista {

    private static final BigDecimal TAXA_DESCONTO = new BigDecimal("0.05");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public ResultadoPagamento aplicar(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Dinheiro.percentual(totalPedido, TAXA_DESCONTO);
        BigDecimal totalFinal = Dinheiro.centavos(totalPedido.subtract(desconto));
        return new ResultadoPagamento(totalFinal, totalFinal);
    }
}
