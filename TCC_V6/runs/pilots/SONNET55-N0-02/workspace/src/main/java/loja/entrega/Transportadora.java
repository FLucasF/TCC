package loja.entrega;

import java.math.BigDecimal;

import loja.entrega.ModalidadeEntrega.Cotacao;

/** Fonte de cotações de frete externas. Para trocar de transportadora, basta outra implementação. */
public interface Transportadora {

    /**
     * @throws loja.dominio.NegocioException MODALIDADE_INDISPONIVEL se não atende o CEP,
     *                                       TRANSPORTADORA_INDISPONIVEL se estiver fora do ar
     */
    Cotacao cotar(String cep, BigDecimal pesoKg, String servico);
}
