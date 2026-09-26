package br.tcc.checkout.pagamento;

import java.math.BigDecimal;
import br.tcc.checkout.util.Arredondador;

public class PixPagamento implements FormaPagamento {
    @Override
    public BigDecimal calcularTotalFinal(BigDecimal total, Integer parcelas) {
        BigDecimal desconto = Arredondador.arredondar(total.multiply(new BigDecimal("0.05")));
        return Arredondador.arredondar(total.subtract(desconto));
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal total, Integer parcelas) {
        return calcularTotalFinal(total, parcelas);
    }

    @Override
    public boolean parcelavalida(Integer parcelas) {
        return parcelas == 1;
    }
}
