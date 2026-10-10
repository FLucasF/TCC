package com.loja.checkout.domain.modalidade;

import com.loja.checkout.domain.ModalidadeEntrega;
import java.math.BigDecimal;

import static com.loja.checkout.util.Arredondamento.arredondar;

public class Economica implements ModalidadeEntrega {

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return arredondar(
            new BigDecimal("12.00")
                .add(new BigDecimal("2.00").multiply(pesoTotalKg))
        );
    }

    @Override
    public int obterPrazoDias() {
        return 7;
    }

    @Override
    public boolean aceita(BigDecimal pesoTotalKg) {
        return true;
    }
}
