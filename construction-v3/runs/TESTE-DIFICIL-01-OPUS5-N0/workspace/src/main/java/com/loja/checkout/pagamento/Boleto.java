package com.loja.checkout.pagamento;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * BOLETO: tarifa de R$ 3,49 somada ao total, sempre a vista. O pedido so sai
 * depois que o boleto compensa, por isso o prazo aumenta 2 dias. Nao aceitamos
 * boleto quando o total do pedido passa de R$ 1.000,00.
 */
@Component
public class Boleto implements FormaPagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000.00");

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
    public int diasAdicionais() {
        return 2;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas, NivelClube nivelClube) {
        BigDecimal totalFinal = Dinheiro.centavos(totalPedido.add(TARIFA));
        return new ResultadoPagamento(totalFinal, totalFinal);
    }
}
