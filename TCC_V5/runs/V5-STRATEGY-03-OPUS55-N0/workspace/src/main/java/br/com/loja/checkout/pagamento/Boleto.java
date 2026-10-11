package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** À vista, com tarifa bancária; não aceito acima de R$ 1.000,00. */
@Component
public class Boleto implements FormaPagamento {

    private static final BigDecimal TARIFA = Dinheiro.reais("3.49");
    private static final BigDecimal TOTAL_MAXIMO = Dinheiro.reais("1000.00");

    @Override
    public String codigo() {
        return "BOLETO";
    }

    @Override
    public boolean permiteParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return totalPedido.compareTo(TOTAL_MAXIMO) <= 0;
    }

    @Override
    public Pagamento calcular(BigDecimal totalPedido, int parcelas) {
        return Pagamento.aVista(totalPedido.add(TARIFA));
    }
}
