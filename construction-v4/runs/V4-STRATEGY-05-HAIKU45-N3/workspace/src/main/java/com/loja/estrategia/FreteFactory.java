package com.loja.estrategia;

import com.loja.enums.TipoEntrega;

public class FreteFactory {
    public static EstrategiaFrete criar(TipoEntrega tipo) {
        return switch (tipo) {
            case ECONOMICA -> new FreteEconomica();
            case EXPRESSA -> new FreteExpressa();
            case RETIRADA_LOJA -> new FreteRetiradaLoja();
            case MOTOBOY -> new FreteMotoboy();
        };
    }
}
