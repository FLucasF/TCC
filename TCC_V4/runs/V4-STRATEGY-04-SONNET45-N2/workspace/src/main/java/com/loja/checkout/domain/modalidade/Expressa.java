package com.loja.checkout.domain.modalidade;

import com.loja.checkout.domain.ModalidadeEntrega;
import java.math.BigDecimal;

import static com.loja.checkout.util.Arredondamento.arredondar;

public class Expressa implements ModalidadeEntrega {

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return arredondar(
            new BigDecimal("25.00")
                .add(new BigDecimal("4.50").multiply(pesoTotalKg))
        );
    }

    @Override
    public int obterPrazoDias() {
        return 2;
    }

    @Override
    public boolean aceita(BigDecimal pesoTotalKg) {
        return true;
    }
}
