package loja.entrega;

import java.math.BigDecimal;

import loja.dominio.Regiao;

/** Uma opção de entrega. Para criar outra, basta um novo @Component que implemente esta interface. */
public interface ModalidadeEntrega {

    String codigo();

    /** Calcula frete e prazo; lança NegocioException se a opção não atende ou a transportadora falha. */
    Cotacao cotar(ContextoEntrega contexto);

    /** Se false, benefícios do clube que zeram o frete não valem para esta opção. */
    default boolean aceitaIsencaoDoClube() {
        return true;
    }

    /** Se false, a opção não tem frete (ex.: retirada na loja). */
    default boolean temFrete() {
        return true;
    }

    record Cotacao(BigDecimal valor, int prazoDias) {
    }

    record ContextoEntrega(String cep, Regiao regiao, BigDecimal pesoKg) {
    }
}
