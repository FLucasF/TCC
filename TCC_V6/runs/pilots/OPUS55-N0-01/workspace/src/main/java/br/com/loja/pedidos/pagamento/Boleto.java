package br.com.loja.pedidos.pagamento;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.loja.pedidos.comum.Dinheiro;

/** À vista, com a tarifa do banco; só para pedidos de até R$ 1.000,00. */
@Component
public class Boleto implements FormaPagamento {

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
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas, boolean usouCupom) {
        return ResultadoPagamento.aVista(totalPedido.add(TARIFA));
    }
}
