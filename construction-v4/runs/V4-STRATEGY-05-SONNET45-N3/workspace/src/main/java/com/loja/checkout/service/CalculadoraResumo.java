package com.loja.checkout.service;

import com.loja.checkout.domain.DadosCalculo;
import com.loja.checkout.domain.NivelClube;
import com.loja.checkout.domain.Regiao;
import com.loja.checkout.domain.cupom.Cupom;
import com.loja.checkout.domain.entrega.ModalidadeEntrega;
import com.loja.checkout.domain.pagamento.FormaPagamento;
import com.loja.checkout.model.dto.ItemPedido;
import com.loja.checkout.model.dto.PedidoRequest;
import com.loja.checkout.model.dto.ResumoResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CalculadoraResumo {

    public ResumoResponse calcular(
        PedidoRequest pedido,
        ModalidadeEntrega modalidadeEntrega,
        Cupom cupom,
        FormaPagamento formaPagamento,
        NivelClube nivelClube,
        Regiao regiao
    ) {
        DadosCalculo dados = new DadosCalculo();
        dados.setItens(pedido.itens());

        int numeroParcelas = pedido.parcelas() != null ? pedido.parcelas() : 1;
        dados.setParcelas(numeroParcelas);

        calcularSubtotalProdutos(dados);
        calcularFrete(dados, modalidadeEntrega, nivelClube);
        aplicarCupom(dados, cupom);
        calcularSeguro(dados, regiao);
        calcularTotalPedido(dados);
        aplicarAjustePagamento(dados, formaPagamento);
        calcularBeneficiosClube(dados, nivelClube);

        return new ResumoResponse(
            dados.getSubtotalProdutos(),
            dados.getDescontoCupom(),
            dados.getFrete(),
            dados.getPrazoEntregaDias(),
            dados.getSeguro(),
            dados.getAjustePagamento(),
            dados.getTotalFinal(),
            dados.getParcelas(),
            dados.getValorParcela(),
            dados.getCreditoProximaCompra(),
            dados.getBrinde()
        );
    }

    private void calcularSubtotalProdutos(DadosCalculo dados) {
        BigDecimal subtotal = dados.getItens().stream()
            .map(item -> item.precoUnitario()
                .multiply(new BigDecimal(item.quantidade()))
                .setScale(2, RoundingMode.HALF_EVEN))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        dados.setSubtotalProdutos(subtotal);
    }

    private void aplicarCupom(DadosCalculo dados, Cupom cupom) {
        if (cupom == null) {
            dados.setDescontoCupom(new BigDecimal("0.00"));
        } else {
            cupom.aplicarDesconto(dados);
        }
    }

    private void calcularFrete(DadosCalculo dados, ModalidadeEntrega modalidade, NivelClube nivelClube) {
        BigDecimal frete = modalidade.calcularFrete(dados.getItens());

        if (nivelClube.temFreteGratis()) {
            frete = new BigDecimal("0.00");
        }

        dados.setFrete(frete);
        dados.setPrazoEntregaDias(modalidade.getPrazoEntregaDias());
    }

    private void calcularSeguro(DadosCalculo dados, Regiao regiao) {
        BigDecimal seguro = dados.getSubtotalProdutos()
            .multiply(regiao.getTaxa())
            .setScale(2, RoundingMode.HALF_EVEN);

        dados.setSeguro(seguro);
    }

    private void calcularTotalPedido(DadosCalculo dados) {
        BigDecimal total = dados.getSubtotalProdutos()
            .subtract(dados.getDescontoCupom())
            .add(dados.getFrete())
            .add(dados.getSeguro());

        dados.setTotalPedido(total);
    }

    private void aplicarAjustePagamento(DadosCalculo dados, FormaPagamento formaPagamento) {
        BigDecimal ajuste = formaPagamento.calcularAjuste(
            dados.getTotalPedido(),
            dados.getParcelas()
        );

        dados.setAjustePagamento(ajuste);

        BigDecimal totalFinal = dados.getTotalPedido().add(ajuste);
        dados.setTotalFinal(totalFinal);

        BigDecimal valorParcela = formaPagamento.calcularValorParcela(
            totalFinal,
            dados.getParcelas()
        );
        dados.setValorParcela(valorParcela);
    }

    private void calcularBeneficiosClube(DadosCalculo dados, NivelClube nivelClube) {
        BigDecimal credito = nivelClube.calcularCredito(dados.getSubtotalProdutos());
        dados.setCreditoProximaCompra(credito);

        boolean brinde = nivelClube.ganharBrinde(dados.getSubtotalProdutos());
        dados.setBrinde(brinde);
    }
}
