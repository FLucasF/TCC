package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Pix implements FormaPagamento {

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = totalPedido.subtract(Dinheiro.percentual(totalPedido, "5"));
        return new Cobranca(totalFinal, totalFinal);
    }
}
