package com.loja.checkout.service.entrega;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class Motoboy implements ModalidadeEntrega {
    @Override public String getCodigo() { return "MOTOBOY"; }
    @Override public boolean aceitaPedido(double pesoTotalKg) { return pesoTotalKg <= 5.0; }
    @Override public BigDecimal calcularFrete(double pesoTotalKg) { return BigDecimal.valueOf(18.00).setScale(2); }
    @Override public int getPrazoDias() { return 0; }
}
