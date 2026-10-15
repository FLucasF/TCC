package loja.checkout.pagamento;

import java.math.BigDecimal;
import loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

@Component
class Boleto implements FormaPagamento {
    private static final BigDecimal LIMITE = new BigDecimal("1000.00");
    private static final BigDecimal TARIFA = new BigDecimal("3.49");

    @Override
    public String codigo() {
        return "BOLETO";
    }

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return totalPedido.compareTo(LIMITE) <= 0;
    }

    @Override
    public Liquidacao liquidar(BigDecimal totalPedido, int parcelas) {
        BigDecimal valor = Dinheiro.arredondar(totalPedido.add(TARIFA));
        return new Liquidacao(valor, valor);
    }
}
