package com.loja.checkout.entrega;

import java.math.BigDecimal;

/**
 * Uma opcao de entrega. Novas transportadoras/opcoes sao adicionadas
 * implementando esta interface e registrando um bean - nao exige alterar
 * as demais modalidades ja existentes.
 */
public interface ModalidadeEntrega {

    String codigo();

    int prazoDias();

    boolean disponivelPara(BigDecimal pesoTotalKg);

    BigDecimal calcularFrete(BigDecimal pesoTotalKg);
}
