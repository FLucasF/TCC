package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.dominio.Erro;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Uma forma de entrega: tem seu jeito de cobrar, seu prazo e suas limitacoes.
 * Para oferecer uma transportadora nova, basta criar uma implementacao e
 * registra-la no catalogo com o codigo que o site envia.
 */
public interface Entrega {

    Catalogo<Entrega> CATALOGO = Catalogo.de(Erro.MODALIDADE_INVALIDA, Map.of(
            "ECONOMICA", new TarifaPorPeso(new BigDecimal("12.00"), new BigDecimal("2.00"), 7),
            "EXPRESSA", new TarifaPorPeso(new BigDecimal("25.00"), new BigDecimal("4.50"), 2),
            "RETIRADA_LOJA", new RetiradaNaLoja(),
            "MOTOBOY", new EntregaMotoboy()));

    Frete calcular(BigDecimal pesoKg);

    /** Sem limitacao de peso, salvo quando a modalidade disser o contrario. */
    default boolean atende(BigDecimal pesoKg) {
        return true;
    }
}
