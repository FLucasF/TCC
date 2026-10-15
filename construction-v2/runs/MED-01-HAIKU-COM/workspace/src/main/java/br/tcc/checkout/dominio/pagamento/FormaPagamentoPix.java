package br.tcc.checkout.dominio.pagamento;

import br.tcc.checkout.util.Arredondador;
import java.math.BigDecimal;

public class FormaPagamentoPix implements FormaPagamento {
    @Override
    public String getCodigo() {
        return "PIX";
    }

    @Override
    public boolean ehValida(Integer parcelas, BigDecimal totalPedido) {
        return parcelas == null || parcelas == 1;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, Integer parcelas) {
        BigDecimal desconto = totalPedido.multiply(new BigDecimal("0.05"));
        desconto = Arredondador.arredondarParaCentavos(desconto);

        BigDecimal totalFinal = totalPedido.subtract(desconto);
        totalFinal = Arredondador.arredondarParaCentavos(totalFinal);

        return new ResultadoPagamento(totalFinal, totalFinal, desconto.negate());
    }
}
