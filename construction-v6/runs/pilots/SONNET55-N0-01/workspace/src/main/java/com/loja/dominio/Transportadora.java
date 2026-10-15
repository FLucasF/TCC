package com.loja.dominio;

import java.math.BigDecimal;
import java.util.Optional;

public interface Transportadora {

    record Cotacao(BigDecimal valor, int prazoDias) {
    }

    /**
     * @return a cotação, ou vazio se a transportadora não atende o CEP
     * @throws ErroNegocio TRANSPORTADORA_INDISPONIVEL se ela estiver fora do ar
     */
    Optional<Cotacao> cotar(String cep, BigDecimal pesoKg, String servico);
}
