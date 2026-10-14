package br.com.loja.pedidos.checkout.pagamento;

import br.com.loja.pedidos.comum.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** À vista, com a tarifa do banco somada ao total. Não aceito acima de R$ 1.000,00. */
@Component
public class Boleto implements FormaPagamento {

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
    public boolean disponivelPara(BigDecimal totalPedido) {
        return totalPedido.compareTo(TOTAL_MAXIMO) <= 0;
    }

    @Override
    public CondicaoPagamento condicao(BigDecimal totalPedido, int parcelas, boolean usouCupom) {
        return CondicaoPagamento.aVista(totalPedido.add(TARIFA));
    }
}
