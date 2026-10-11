package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class PagamentoBoleto implements FormaPagamento {

    private static final BigDecimal LIMITE = Dinheiro.valor("1000.00");
    private static final BigDecimal TARIFA = Dinheiro.valor("3.49");

    @Override
    public String codigo() {
        return "BOLETO";
    }

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return totalPedido.compareTo(LIMITE) <= 0;
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        BigDecimal total = totalPedido.add(TARIFA);
        return new Cobranca(total, total);
    }
}
