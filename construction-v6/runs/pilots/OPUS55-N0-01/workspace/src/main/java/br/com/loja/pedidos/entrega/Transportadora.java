package br.com.loja.pedidos.entrega;

import java.math.BigDecimal;
import java.util.Optional;

/** Uma transportadora parceira que cota frete e prazo pelo sistema dela. */
public interface Transportadora {

    /**
     * @return a cotação, ou vazio quando a transportadora não atende o CEP
     * @throws TransportadoraIndisponivelException quando a transportadora está fora do ar
     */
    Optional<CotacaoEntrega> cotar(String cep, BigDecimal pesoKg, String servico);
}
