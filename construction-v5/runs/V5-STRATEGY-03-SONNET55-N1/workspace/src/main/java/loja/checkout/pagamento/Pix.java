package loja.checkout.pagamento;

import java.math.BigDecimal;
import loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

@Component
public class Pix implements FormaPagamento {

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
        BigDecimal total = totalPedido.subtract(Dinheiro.percentual(totalPedido, "0.05"));
        return new Cobranca(total, total);
    }
}
