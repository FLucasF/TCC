package br.tcc.checkout.dominio.entrega;

import br.tcc.checkout.util.Arredondador;
import java.math.BigDecimal;

public class ModalidadeExpressa implements ModalidadeEntrega {
    @Override
    public String getCodigo() {
        return "EXPRESSA";
    }

    @Override
    public int getPrazo() {
        return 2;
    }

    @Override
    public boolean ehDisponivel(BigDecimal pesoTotalKg) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        BigDecimal base = new BigDecimal("25.00");
        BigDecimal porKg = pesoTotalKg.multiply(new BigDecimal("4.50"));
        BigDecimal frete = base.add(porKg);
        return Arredondador.arredondarParaCentavos(frete);
    }
}
