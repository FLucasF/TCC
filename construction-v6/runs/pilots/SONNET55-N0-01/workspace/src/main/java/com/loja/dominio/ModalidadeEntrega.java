package com.loja.dominio;

import java.math.BigDecimal;

/** Uma opção de entrega. Para criar uma nova, basta uma nova classe @Component. */
public interface ModalidadeEntrega {

    record Contexto(String cep, BigDecimal pesoKg, Regiao regiao) {
    }

    record Cotacao(BigDecimal frete, int prazoDias) {
    }

    String codigo();

    /** @throws ErroNegocio MODALIDADE_INDISPONIVEL ou TRANSPORTADORA_INDISPONIVEL */
    Cotacao cotar(Contexto contexto);

    /** Se o benefício de frete grátis do clube vale para esta opção. */
    default boolean aceitaFreteGratisDoClube() {
        return true;
    }
}
