package com.loja.checkout.catalogo;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dominio.ErroPedido;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Monta um catalogo por tipo de opcao, com as implementacoes que existem.
 * Uma opcao nova entra so por existir.
 */
@Configuration
public class CatalogosConfig {

    @Bean
    public Catalogo<NivelClube> niveisClube(List<NivelClube> niveis) {
        return new Catalogo<>(niveis, ErroPedido.NIVEL_CLUBE_INVALIDO);
    }

    @Bean
    public Catalogo<Regiao> regioes() {
        return new Catalogo<>(List.of(Regiao.values()), ErroPedido.REGIAO_INVALIDA);
    }

    @Bean
    public Catalogo<ModalidadeEntrega> modalidadesEntrega(List<ModalidadeEntrega> modalidades) {
        return new Catalogo<>(modalidades, ErroPedido.MODALIDADE_INVALIDA);
    }

    @Bean
    public Catalogo<Cupom> cupons(List<Cupom> cupons) {
        return new Catalogo<>(cupons, ErroPedido.CUPOM_INVALIDO);
    }

    @Bean
    public Catalogo<FormaPagamento> formasPagamento(List<FormaPagamento> formas) {
        return new Catalogo<>(formas, ErroPedido.FORMA_PAGAMENTO_INVALIDA);
    }
}
