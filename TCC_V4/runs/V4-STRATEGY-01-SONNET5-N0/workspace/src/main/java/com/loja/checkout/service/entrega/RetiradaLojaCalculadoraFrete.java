package com.loja.checkout.service.entrega;

import com.loja.checkout.enums.ModalidadeEntrega;
import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RetiradaLojaCalculadoraFrete implements CalculadoraFrete {

    @Override
    public ModalidadeEntrega getModalidade() {
        return ModalidadeEntrega.RETIRADA_LOJA;
    }

    @Override
    public boolean disponivelPara(BigDecimal pesoTotalKg) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return Dinheiro.zero();
    }

    @Override
    public int prazoDias() {
        return 1;
    }
}
