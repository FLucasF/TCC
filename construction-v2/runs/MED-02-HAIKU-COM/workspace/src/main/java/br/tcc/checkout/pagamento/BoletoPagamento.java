package br.tcc.checkout.pagamento;

import java.math.BigDecimal;
import br.tcc.checkout.util.Arredondador;

public class BoletoPagamento implements FormaPagamento {
    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    public static final BigDecimal LIMITE_MAXIMO = new BigDecimal("1000.00");

    @Override
    public BigDecimal calcularTotalFinal(BigDecimal total, Integer parcelas) {
        return Arredondador.arredondar(total.add(TARIFA));
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
