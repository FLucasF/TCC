package com.loja.checkout;

import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Monta os catalogos com todas as opcoes registradas no sistema. */
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
    public Catalogo<FormaPagamento> catalogoPagamentos(List<FormaPagamento> formas) {
        return new Catalogo<>(formas);
    }
}
