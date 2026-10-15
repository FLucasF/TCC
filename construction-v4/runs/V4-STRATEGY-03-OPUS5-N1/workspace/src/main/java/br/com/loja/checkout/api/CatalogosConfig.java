package br.com.loja.checkout.api;

import br.com.loja.checkout.catalogo.Catalogo;
import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import br.com.loja.checkout.pagamento.FormaPagamento;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Reune os casos de cada variacao nos catalogos usados para escolher por codigo. */
@Configuration
public class CatalogosConfig {

    @Bean
    public Catalogo<ModalidadeEntrega> catalogoEntregas(List<ModalidadeEntrega> modalidades) {
        return new Catalogo<>(modalidades);
    }

    @Bean
    public Catalogo<Cupom> catalogoCupons(List<Cupom> cupons) {
        return new Catalogo<>(cupons);
    }

    @Bean
    public Catalogo<NivelClube> catalogoNiveisClube(List<NivelClube> niveis) {
        return new Catalogo<>(niveis);
    }

    @Bean
    public Catalogo<FormaPagamento> catalogoFormasPagamento(List<FormaPagamento> formas) {
        return new Catalogo<>(formas);
    }
}
