package com.loja.checkout.config;

import com.loja.checkout.aplicacao.Catalogos;
import com.loja.checkout.aplicacao.regras.CupomAplicavel;
import com.loja.checkout.aplicacao.regras.CupomConhecido;
import com.loja.checkout.aplicacao.regras.FormaPagamentoConhecida;
import com.loja.checkout.aplicacao.regras.FormaPagamentoDisponivel;
import com.loja.checkout.aplicacao.regras.ItensValidos;
import com.loja.checkout.aplicacao.regras.ModalidadeConhecida;
import com.loja.checkout.aplicacao.regras.ModalidadeDisponivel;
import com.loja.checkout.aplicacao.regras.NivelClubeConhecido;
import com.loja.checkout.aplicacao.regras.ParcelamentoPermitido;
import com.loja.checkout.aplicacao.regras.RegrasDoPedido;
import com.loja.checkout.aplicacao.regras.RegiaoConhecida;
import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.dominio.clube.NivelClube;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracaoDoCheckout {

    @Bean
    public Catalogos catalogos(
            List<ModalidadeEntrega> entregas,
            List<Cupom> cupons,
            List<NivelClube> niveis,
            List<FormaPagamento> pagamentos) {
        return new Catalogos(
                new Catalogo<>(entregas),
                new Catalogo<>(cupons),
                new Catalogo<>(niveis),
                new Catalogo<>(pagamentos));
    }

    /** A ordem de conferencia do pedido: vale o primeiro problema encontrado. */
    @Bean
    public RegrasDoPedido regrasDoPedido() {
        return new RegrasDoPedido(List.of(
                new ItensValidos(),
                new NivelClubeConhecido(),
                new RegiaoConhecida(),
                new ModalidadeConhecida(),
                new ModalidadeDisponivel(),
                new CupomConhecido(),
                new CupomAplicavel(),
                new FormaPagamentoConhecida(),
                new ParcelamentoPermitido(),
                new FormaPagamentoDisponivel()));
    }
}
