package com.loja.service;

import com.loja.clube.NivelClube;
import com.loja.cupom.Cupom;
import com.loja.cupom.FabricaCupom;
import com.loja.entrega.Entrega;
import com.loja.entrega.FabricaEntrega;
import com.loja.model.ItemPedido;
import com.loja.model.RequisicaoResumo;
import com.loja.model.RespostaErro;
import com.loja.model.RespostaResumo;
import com.loja.pagamento.FormaPagamento;
import com.loja.pagamento.FabricaFormaPagamento;
import com.loja.regiao.Regiao;
import com.loja.util.Arredondamento;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ResumoCheckoutService {

    public Object calcularResumo(RequisicaoResumo requisicao) {
        Object validacao = validarRequisicao(requisicao);
        if (validacao != null) {
            return validacao;
        }

        RespostaResumo resposta = new RespostaResumo();

        BigDecimal subtotalProdutos = calcularSubtotal(requisicao.getItens());
        subtotalProdutos = Arredondamento.arredondarParaPar(subtotalProdutos);
        resposta.setSubtotalProdutos(subtotalProdutos);

        NivelClube nivelClube = NivelClube.porCodigo(requisicao.getNivelClube());
        Entrega entrega = FabricaEntrega.criar(requisicao.getModalidadeEntrega());

        double pesoTotal = calcularPesoTotal(requisicao.getItens());

        BigDecimal frete = new BigDecimal("0.00");
        if (!nivelClube.isNaoPageFrete()) {
            frete = entrega.calcularFrete(pesoTotal);
        }
        resposta.setFrete(frete);
        resposta.setPrazoEntregaDias(entrega.obterPrazo());

        BigDecimal descontoCupom = new BigDecimal("0.00");
        if (requisicao.getCupom() != null && !requisicao.getCupom().isEmpty()) {
            Cupom cupom = FabricaCupom.criar(requisicao.getCupom());
            if (cupom != null && cupom.podeAplicar(requisicao.getItens(), subtotalProdutos)) {
                descontoCupom = cupom.calcularDesconto(requisicao.getItens(), subtotalProdutos, frete);
            }
        }
        resposta.setDescontoCupom(descontoCupom);

        Regiao regiao = Regiao.porCodigo(requisicao.getRegiao());
        BigDecimal seguro = calcularSeguro(subtotalProdutos, regiao);
        resposta.setSeguro(seguro);

        BigDecimal totalPedido = subtotalProdutos
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro);
        totalPedido = Arredondamento.arredondarParaPar(totalPedido);

        FormaPagamento formaPagamento = FabricaFormaPagamento.criar(requisicao.getFormaPagamento());
        Integer parcelas = requisicao.getParcelas() != null ? requisicao.getParcelas() : 1;

        BigDecimal ajustePagamento = formaPagamento.calcularAjuste(totalPedido, parcelas);
        resposta.setAjustePagamento(ajustePagamento);

        BigDecimal totalFinal = totalPedido.add(ajustePagamento);
        totalFinal = Arredondamento.arredondarParaPar(totalFinal);
        resposta.setTotalFinal(totalFinal);

        BigDecimal valorParcela = calcularValorParcela(totalFinal, parcelas);
        resposta.setParcelas(parcelas);
        resposta.setValorParcela(valorParcela);

        BigDecimal credito = calcularCredito(subtotalProdutos, nivelClube);
        resposta.setCreditoProximaCompra(credito);

        boolean brinde = nivelClube == NivelClube.OURO && subtotalProdutos.compareTo(BigDecimal.valueOf(500.00)) > 0;
        resposta.setBrinde(brinde);

        return resposta;
    }

    private Object validarRequisicao(RequisicaoResumo requisicao) {
        if (requisicao.getItens() == null || requisicao.getItens().isEmpty()) {
            return new RespostaErro("PEDIDO_INVALIDO");
        }

        for (ItemPedido item : requisicao.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0 ||
                    item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                    item.getPesoKg() == null || item.getPesoKg() < 0) {
                return new RespostaErro("PEDIDO_INVALIDO");
            }
        }

        NivelClube nivelClube = NivelClube.porCodigo(requisicao.getNivelClube());
        if (nivelClube == null) {
            return new RespostaErro("NIVEL_CLUBE_INVALIDO");
        }

        Regiao regiao = Regiao.porCodigo(requisicao.getRegiao());
        if (regiao == null) {
            return new RespostaErro("REGIAO_INVALIDA");
        }

        Entrega entrega = FabricaEntrega.criar(requisicao.getModalidadeEntrega());
        if (entrega == null) {
            return new RespostaErro("MODALIDADE_INVALIDA");
        }

        double pesoTotal = calcularPesoTotal(requisicao.getItens());
        if (!entrega.estaDisponivel(pesoTotal)) {
            return new RespostaErro("MODALIDADE_INDISPONIVEL");
        }

        if (requisicao.getCupom() != null && !requisicao.getCupom().isEmpty()) {
            Cupom cupom = FabricaCupom.criar(requisicao.getCupom());
            if (cupom == null) {
                return new RespostaErro("CUPOM_INVALIDO");
            }

            BigDecimal subtotal = calcularSubtotal(requisicao.getItens());
            subtotal = Arredondamento.arredondarParaPar(subtotal);
            if (!cupom.podeAplicar(requisicao.getItens(), subtotal)) {
                return new RespostaErro("CUPOM_NAO_APLICAVEL");
            }
        }

        FormaPagamento formaPagamento = FabricaFormaPagamento.criar(requisicao.getFormaPagamento());
        if (formaPagamento == null) {
            return new RespostaErro("FORMA_PAGAMENTO_INVALIDA");
        }

        Integer parcelas = requisicao.getParcelas() != null ? requisicao.getParcelas() : 1;
        if (parcelas < 1 || parcelas > formaPagamento.getMaximoParcelas() ||
                (!formaPagamento.podeParcelar() && parcelas != 1)) {
            return new RespostaErro("PARCELAMENTO_INVALIDO");
        }

        BigDecimal subtotal = calcularSubtotal(requisicao.getItens());
        subtotal = Arredondamento.arredondarParaPar(subtotal);

        BigDecimal frete = new BigDecimal("0.00");
        if (!nivelClube.isNaoPageFrete()) {
            frete = entrega.calcularFrete(pesoTotal);
        }

        BigDecimal descontoCupom = new BigDecimal("0.00");
        if (requisicao.getCupom() != null && !requisicao.getCupom().isEmpty()) {
            Cupom cupom = FabricaCupom.criar(requisicao.getCupom());
            if (cupom != null && cupom.podeAplicar(requisicao.getItens(), subtotal)) {
                descontoCupom = cupom.calcularDesconto(requisicao.getItens(), subtotal, frete);
            }
        }

        BigDecimal seguro = calcularSeguro(subtotal, regiao);
        BigDecimal totalPedido = subtotal
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro);
        totalPedido = Arredondamento.arredondarParaPar(totalPedido);

        if (!formaPagamento.estaDisponivel(totalPedido)) {
            return new RespostaErro("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        return null;
    }

    private BigDecimal calcularSubtotal(List<ItemPedido> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            BigDecimal precoItem = BigDecimal.valueOf(item.getPrecoUnitario())
                    .multiply(BigDecimal.valueOf(item.getQuantidade()));
            subtotal = subtotal.add(precoItem);
        }
        return subtotal;
    }

    private double calcularPesoTotal(List<ItemPedido> itens) {
        double peso = 0;
        for (ItemPedido item : itens) {
            peso += item.getPesoKg() * item.getQuantidade();
        }
        return peso;
    }

    private BigDecimal calcularSeguro(BigDecimal subtotal, Regiao regiao) {
        BigDecimal seguro = subtotal.multiply(regiao.getPercentualSeguro());
        return Arredondamento.arredondarParaPar(seguro);
    }

    private BigDecimal calcularCredito(BigDecimal subtotal, NivelClube nivelClube) {
        BigDecimal credito = subtotal.multiply(nivelClube.getPercentualCredito());
        return Arredondamento.arredondarParaPar(credito);
    }

    private BigDecimal calcularValorParcela(BigDecimal total, Integer parcelas) {
        if (parcelas == null || parcelas <= 0) {
            parcelas = 1;
        }
        BigDecimal parcela = total.divide(BigDecimal.valueOf(parcelas), 10, java.math.RoundingMode.HALF_EVEN);
        return Arredondamento.arredondarParaPar(parcela);
    }
}
