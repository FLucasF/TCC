package br.com.loja.checkout;

import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.dominio.Catalogo;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import br.com.loja.checkout.pagamento.FormaPagamento;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class CatalogosConfig {

    @Bean
    Catalogo<ModalidadeEntrega> catalogoModalidades(List<ModalidadeEntrega> modalidades) {
        return new Catalogo<>(modalidades);
    }

    @Bean
    Catalogo<Cupom> catalogoCupons(List<Cupom> cupons) {
        return new Catalogo<>(cupons);
    }

    @Bean
    Catalogo<NivelClube> catalogoNiveisClube(List<NivelClube> niveis) {
        return new Catalogo<>(niveis);
    }

    @Bean
    Catalogo<FormaPagamento> catalogoFormasPagamento(List<FormaPagamento> formas) {
        return new Catalogo<>(formas);
    }
}
