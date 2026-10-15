package com.loja.entrega;

import com.loja.util.Arredondamento;
import java.math.BigDecimal;

public class EntregaEconomica implements Entrega {
    @Override
    public BigDecimal calcularFrete(double pesoTotal) {
        double frete = 12.00 + (2.00 * pesoTotal);
        return Arredondamento.arredondarParaPar(frete);
    }

    @Override
    public Integer obterPrazo() {
        return 7;
    }

    @Override
    public boolean estaDisponivel(double pesoTotal) {
        return true;
    }
}
