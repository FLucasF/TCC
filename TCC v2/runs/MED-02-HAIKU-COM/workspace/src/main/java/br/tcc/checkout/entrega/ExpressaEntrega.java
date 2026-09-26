package br.tcc.checkout.entrega;

import java.math.BigDecimal;
import br.tcc.checkout.util.Arredondador;

public class ExpressaEntrega implements Entrega {
    @Override
    public BigDecimal calcularFrete(Double pesoTotal) {
        BigDecimal base = new BigDecimal("25.00");
        BigDecimal porKg = new BigDecimal("4.50");
        BigDecimal peso = new BigDecimal(pesoTotal);
        return Arredondador.arredondar(base.add(porKg.multiply(peso)));
    }

    @Override
    public Integer getPrazoEntregaDias() {
        return 2;
    }

    @Override
    public boolean podeAtender(Double pesoTotal) {
        return true;
    }
}
