package com.loja.entrega;

import com.loja.util.Arredondamento;
import java.math.BigDecimal;

public class EntregaExpressa implements Entrega {
    @Override
    public BigDecimal calcularFrete(double pesoTotal) {
        double frete = 25.00 + (4.50 * pesoTotal);
        return Arredondamento.arredondarParaPar(frete);
    }

    @Override
    public Integer obterPrazo() {
        return 2;
    }

    @Override
    public boolean estaDisponivel(double pesoTotal) {
        return true;
    }
}
