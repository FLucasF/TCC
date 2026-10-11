package com.loja.checkout.service.entrega;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class RetiradaLoja implements ModalidadeEntrega {
    @Override public String getCodigo() { return "RETIRADA_LOJA"; }
    @Override public boolean aceitaPedido(double pesoTotalKg) { return true; }
    @Override public BigDecimal calcularFrete(double pesoTotalKg) { return BigDecimal.ZERO.setScale(2); }
    @Override public int getPrazoDias() { return 1; }
}
