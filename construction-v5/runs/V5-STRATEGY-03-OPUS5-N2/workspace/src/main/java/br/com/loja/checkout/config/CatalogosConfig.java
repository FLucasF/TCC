package br.com.loja.checkout.config;

import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.dominio.Catalogo;
import br.com.loja.checkout.dominio.Regiao;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import br.com.loja.checkout.pagamento.FormaPagamento;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Reune as opcoes de cada tipo num catalogo. Opcao nova entra so se registrando aqui. */
@Configuration
public class CatalogosConfig {

    @Bean
    public Catalogo<ModalidadeEntrega> catalogoModalidades(List<ModalidadeEntrega> modalidades) {
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

    @Bean
    public Catalogo<Regiao> catalogoRegioes() {
        return new Catalogo<>(List.of(Regiao.values()));
    }
}
