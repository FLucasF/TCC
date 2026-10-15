package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Pix implements FormaPagamento {

    public String codigo() {
        return "PIX";
    }

    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    public boolean aceitaTotal(BigDecimal totalPedido) {
        return true;
    }

    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        BigDecimal total = totalPedido.subtract(Dinheiro.percentual(totalPedido, "0.05"));
        return new Cobranca(total, total);
    }
}
