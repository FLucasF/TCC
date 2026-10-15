package com.loja.checkout.entrega;

import java.math.BigDecimal;

/**
 * Cada opcao de entrega tem seu jeito de cobrar, prazo e limitacoes proprios.
 * Novas transportadoras sao adicionadas implementando esta interface e
 * registrando o bean no Spring, sem alterar o restante do sistema.
 */
public interface EntregaStrategy {

    String codigo();

    int prazoDias();

    boolean disponivelPara(BigDecimal pesoTotalKg, BigDecimal subtotalProdutos);

    BigDecimal calcularFrete(BigDecimal pesoTotalKg, BigDecimal subtotalProdutos);
}
