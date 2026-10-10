package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.pedido.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Boleto implements FormaPagamento {

    private static final BigDecimal TARIFA = Dinheiro.reais("3.49");
    private static final BigDecimal TOTAL_MAXIMO = Dinheiro.reais("1000.00");

    @Override
    public String codigo() {
        return "BOLETO";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return totalPedido.compareTo(TOTAL_MAXIMO) <= 0;
    }

    @Override
    public Pagamento pagar(BigDecimal totalPedido, int parcelas) {
        return Pagamento.aVista(totalPedido.add(TARIFA));
    }
}
