package br.tcc.checkout.entrega;

import java.math.BigDecimal;
import br.tcc.checkout.util.Arredondador;

public class EconomicaEntrega implements Entrega {
    @Override
    public BigDecimal calcularFrete(Double pesoTotal) {
        BigDecimal base = new BigDecimal("12.00");
        BigDecimal porKg = new BigDecimal("2.00");
        BigDecimal peso = new BigDecimal(pesoTotal);
        return Arredondador.arredondar(base.add(porKg.multiply(peso)));
    }

    @Override
    public Integer getPrazoEntregaDias() {
        return 7;
    }

    @Override
    public boolean podeAtender(Double pesoTotal) {
        return true;
    }
}
