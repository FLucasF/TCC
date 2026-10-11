package br.com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;
import java.util.List;

import br.com.loja.checkout.dominio.Catalogo;
import br.com.loja.checkout.dominio.MotivoRecusa;

/**
 * Uma opção de entrega. Cada parceria nova é uma implementação nova, com seu
 * jeito de cobrar, seu prazo e suas limitações, registrada no catálogo.
 */
public interface ModalidadeEntrega {

    Catalogo<ModalidadeEntrega> CATALOGO = Catalogo.de(
            MotivoRecusa.MODALIDADE_INVALIDA,
            ModalidadeEntrega::codigo,
            List.of(new Economica(), new Expressa(), new RetiradaLoja(), new Motoboy()));

    String codigo();

    /** Se a opção atende um pedido com esse peso. */
    boolean atende(BigDecimal pesoKg);

    BigDecimal frete(BigDecimal pesoKg);

    int prazoDias();
}
