package com.loja.checkout;

import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatalogosConfig {

    @Bean
    public Catalogo<ModalidadeEntrega> catalogoDeEntregas(List<ModalidadeEntrega> modalidades) {
        return new Catalogo<>(modalidades, ModalidadeEntrega::codigo);
    }

    @Bean
    public Catalogo<Cupom> catalogoDeCupons(List<Cupom> cupons) {
        return new Catalogo<>(cupons, Cupom::codigo);
    }

    @Bean
    public Catalogo<FormaPagamento> catalogoDeFormasDePagamento(List<FormaPagamento> formas) {
        return new Catalogo<>(formas, FormaPagamento::codigo);
    }
}
