package com.loja.entrega;

import java.math.BigDecimal;

public interface Entrega {
    BigDecimal calcularFrete(double pesoTotal);
    Integer obterPrazo();
    boolean estaDisponivel(double pesoTotal);
}
