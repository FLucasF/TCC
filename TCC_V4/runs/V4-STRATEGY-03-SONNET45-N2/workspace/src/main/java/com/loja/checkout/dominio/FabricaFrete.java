package com.loja.checkout.dominio;

import com.loja.checkout.dominio.frete.*;
import java.util.Map;

public class FabricaFrete {

    private static final Map<ModalidadeEntrega, CalculadoraFrete> CALCULADORAS = Map.of(
        ModalidadeEntrega.ECONOMICA, new FreteEconomico(),
        ModalidadeEntrega.EXPRESSA, new FreteExpresso(),
        ModalidadeEntrega.RETIRADA_LOJA, new FreteRetiradaLoja(),
        ModalidadeEntrega.MOTOBOY, new FreteMotoboy()
    );

    public CalculadoraFrete obter(ModalidadeEntrega modalidade) {
        return CALCULADORAS.get(modalidade);
    }
}
