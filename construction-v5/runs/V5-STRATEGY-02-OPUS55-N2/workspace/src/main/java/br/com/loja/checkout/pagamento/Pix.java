package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
class Pix implements FormaPagamento {

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
    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public Pagamento pagar(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = totalPedido.subtract(Dinheiro.percentual(totalPedido, PERCENTUAL_DESCONTO));
        return new Pagamento(totalFinal, 1, totalFinal);
    }
}
