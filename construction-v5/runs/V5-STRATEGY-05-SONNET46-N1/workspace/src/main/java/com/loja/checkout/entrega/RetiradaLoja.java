package com.loja.checkout.entrega;

import com.loja.checkout.Arredondamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RetiradaLoja implements ModalidadeEntrega {
    @Override
    public String codigo() { return "RETIRADA_LOJA"; }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return Arredondamento.ZERO;
    }

    @Override
    public int prazoEntregaDias() { return 1; }

    @Override
    public void validarDisponibilidade(BigDecimal pesoTotalKg) {}
}
