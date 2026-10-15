package com.loja.checkout;

import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.dominio.clube.NivelClube;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
