package loja.checkout.pagamento;

import java.math.BigDecimal;
import loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

@Component
class Pix implements FormaPagamento {
    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public Liquidacao liquidar(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Dinheiro.arredondar(totalPedido.multiply(new BigDecimal("0.05")));
        BigDecimal valor = totalPedido.subtract(desconto);
        return new Liquidacao(valor, valor);
    }
}
