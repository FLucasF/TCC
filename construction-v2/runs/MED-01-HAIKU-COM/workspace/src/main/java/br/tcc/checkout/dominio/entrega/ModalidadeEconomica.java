package br.tcc.checkout.dominio.entrega;

import br.tcc.checkout.util.Arredondador;
import java.math.BigDecimal;

public class ModalidadeEconomica implements ModalidadeEntrega {
    @Override
    public String getCodigo() {
        return "ECONOMICA";
    }

    @Override
    public int getPrazo() {
        return 7;
    }

    @Override
    public boolean ehDisponivel(BigDecimal pesoTotalKg) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        BigDecimal base = new BigDecimal("12.00");
        BigDecimal porKg = pesoTotalKg.multiply(new BigDecimal("2.00"));
        BigDecimal frete = base.add(porKg);
        return Arredondador.arredondarParaCentavos(frete);
    }
}
