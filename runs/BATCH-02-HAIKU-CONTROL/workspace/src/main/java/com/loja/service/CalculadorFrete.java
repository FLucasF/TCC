package com.loja.service;

import com.loja.enums.ModalidadeEntrega;
import com.loja.util.ArredondadorMeioParaPar;

import java.math.BigDecimal;

public class CalculadorFrete {

    public static BigDecimal calcular(ModalidadeEntrega modalidade, BigDecimal pesoTotal) {
        BigDecimal frete = modalidade.getTaxa()
            .add(modalidade.getValorPorKg().multiply(pesoTotal));

        return ArredondadorMeioParaPar.arredondar(frete);
    }
}
