package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.calculo.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** À vista, com a tarifa do banco somada ao total. Não aceito acima de R$ 1.000,00. */
@Component
public class PagamentoBoleto implements FormaPagamento {

    private static final BigDecimal TARIFA = Dinheiro.reais("3.49");
    private static final BigDecimal TOTAL_MAXIMO = Dinheiro.reais("1000.00");

    @Override
    public String codigo() {
        return "BOLETO";
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return totalPedido.compareTo(TOTAL_MAXIMO) <= 0;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = totalPedido.add(TARIFA);
        return new ResultadoPagamento(totalFinal, 1, totalFinal);
    }
}
