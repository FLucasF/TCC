package com.loja.checkout.service;

import com.loja.checkout.enums.*;
import com.loja.checkout.model.*;
import com.loja.checkout.strategy.*;
import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class ResumoCalculadora {

    public RespostaResumo calcular(RequisicaoResumo requisicao) throws ValidacaoException {
        validarEntrada(requisicao);

        List<Item> itens = requisicao.getItens();
        Double pesoTotal = calcularPesoTotal(itens);

        BigDecimal subtotal = calcularSubtotal(itens);
        subtotal = Arredondamento.arredondarParaCentavos(subtotal);

        ModalidadeEntrega modalidade = ModalidadeEntrega.fromString(requisicao.getModalidadeEntrega());
        EntregaStrategy estrategiaEntrega = EntregaFactory.criar(modalidade);
        BigDecimal frete = estrategiaEntrega.calcularFrete(pesoTotal);
        frete = Arredondamento.arredondarParaCentavos(frete);
        Integer prazo = estrategiaEntrega.getPrazo();

        NivelClube nivelClube = NivelClube.fromString(requisicao.getNivelClube());
        ClubeStrategy estrategiaClube = ClubeFactory.criar(nivelClube);

        if (estrategiaClube.temFreteGratis()) {
            frete = new BigDecimal("0.00");
        }

        BigDecimal descontoCupom = new BigDecimal("0.00");
        if (requisicao.getCupom() != null && !requisicao.getCupom().isEmpty()) {
            TipoCupom tipoCupom = TipoCupom.fromString(requisicao.getCupom());
            CupomStrategy estrategiaCupom = CupomFactory.criar(tipoCupom);
            descontoCupom = estrategiaCupom.calcularDesconto(subtotal, frete, itens);
        }
        descontoCupom = Arredondamento.arredondarParaCentavos(descontoCupom);

        Regiao regiao = Regiao.fromString(requisicao.getRegiao());
        BigDecimal seguro = SeguroFactory.calcularSeguro(regiao, subtotal);
        seguro = Arredondamento.arredondarParaCentavos(seguro);

        BigDecimal totalAntesAjuste = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        FormaPagamento formaPagamento = FormaPagamento.fromString(requisicao.getFormaPagamento());
        PagamentoStrategy estrategiaPagamento = PagamentoFactory.criar(formaPagamento);
        Integer parcelas = requisicao.getParcelas();

        BigDecimal ajuste = estrategiaPagamento.calcularAjuste(totalAntesAjuste, parcelas);
        ajuste = Arredondamento.arredondarParaCentavos(ajuste);

        BigDecimal totalFinal = totalAntesAjuste.add(ajuste);
        totalFinal = Arredondamento.arredondarParaCentavos(totalFinal);

        BigDecimal valorParcela = estrategiaPagamento.calcularValorParcela(totalFinal, parcelas);
        valorParcela = Arredondamento.arredondarParaCentavos(valorParcela);

        BigDecimal creditoProxima = estrategiaClube.calcularCredito(subtotal);
        creditoProxima = Arredondamento.arredondarParaCentavos(creditoProxima);

        Boolean temBrinde = estrategiaClube.temBrinde(subtotal);

        return new RespostaResumo(
            subtotal, descontoCupom, frete, prazo, seguro, ajuste,
            totalFinal, parcelas, valorParcela, creditoProxima, temBrinde
        );
    }

    private void validarEntrada(RequisicaoResumo requisicao) throws ValidacaoException {
        validarPedido(requisicao);
        validarNivelClube(requisicao);
        validarRegiao(requisicao);
        validarModalidade(requisicao);
        validarCupom(requisicao);
        validarPagamento(requisicao);
    }

    private void validarPedido(RequisicaoResumo requisicao) throws ValidacaoException {
        if (requisicao.getItens() == null || requisicao.getItens().isEmpty()) {
            throw new ValidacaoException("PEDIDO_INVALIDO");
        }

        for (Item item : requisicao.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg() < 0) {
                throw new ValidacaoException("PEDIDO_INVALIDO");
            }
        }
    }

    private void validarNivelClube(RequisicaoResumo requisicao) throws ValidacaoException {
        NivelClube nivelClube = NivelClube.fromString(requisicao.getNivelClube());
        if (nivelClube == null) {
            throw new ValidacaoException("NIVEL_CLUBE_INVALIDO");
        }
    }

    private void validarRegiao(RequisicaoResumo requisicao) throws ValidacaoException {
        Regiao regiao = Regiao.fromString(requisicao.getRegiao());
        if (regiao == null) {
            throw new ValidacaoException("REGIAO_INVALIDA");
        }
    }

    private void validarModalidade(RequisicaoResumo requisicao) throws ValidacaoException {
        ModalidadeEntrega modalidade = ModalidadeEntrega.fromString(requisicao.getModalidadeEntrega());
        if (modalidade == null) {
            throw new ValidacaoException("MODALIDADE_INVALIDA");
        }

        Double pesoTotal = calcularPesoTotal(requisicao.getItens());
        EntregaStrategy estrategiaEntrega = EntregaFactory.criar(modalidade);
        if (!estrategiaEntrega.isDisponivel(pesoTotal)) {
            throw new ValidacaoException("MODALIDADE_INDISPONIVEL");
        }
    }

    private void validarCupom(RequisicaoResumo requisicao) throws ValidacaoException {
        if (requisicao.getCupom() == null || requisicao.getCupom().isEmpty()) {
            return;
        }

        TipoCupom tipoCupom = TipoCupom.fromString(requisicao.getCupom());
        if (tipoCupom == null) {
            throw new ValidacaoException("CUPOM_INVALIDO");
        }

        BigDecimal subtotal = calcularSubtotal(requisicao.getItens());
        CupomStrategy estrategiaCupom = CupomFactory.criar(tipoCupom);
        if (!estrategiaCupom.isAplicavel(subtotal)) {
            throw new ValidacaoException("CUPOM_NAO_APLICAVEL");
        }
    }

    private void validarPagamento(RequisicaoResumo requisicao) throws ValidacaoException {
        FormaPagamento formaPagamento = FormaPagamento.fromString(requisicao.getFormaPagamento());
        if (formaPagamento == null) {
            throw new ValidacaoException("FORMA_PAGAMENTO_INVALIDA");
        }

        Integer parcelas = requisicao.getParcelas();
        PagamentoStrategy estrategiaPagamento = PagamentoFactory.criar(formaPagamento);

        if (parcelas < 1 || parcelas > estrategiaPagamento.getParcelasMaximas()) {
            throw new ValidacaoException("PARCELAMENTO_INVALIDO");
        }

        if (!estrategiaPagamento.isDisponivel(calcularTotalParaValidacao(requisicao))) {
            throw new ValidacaoException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private BigDecimal calcularTotalParaValidacao(RequisicaoResumo requisicao) {
        BigDecimal subtotal = calcularSubtotal(requisicao.getItens());

        Double pesoTotal = calcularPesoTotal(requisicao.getItens());
        ModalidadeEntrega modalidade = ModalidadeEntrega.fromString(requisicao.getModalidadeEntrega());
        EntregaStrategy estrategiaEntrega = EntregaFactory.criar(modalidade);
        BigDecimal frete = estrategiaEntrega.calcularFrete(pesoTotal);

        NivelClube nivelClube = NivelClube.fromString(requisicao.getNivelClube());
        ClubeStrategy estrategiaClube = ClubeFactory.criar(nivelClube);
        if (estrategiaClube.temFreteGratis()) {
            frete = new BigDecimal("0.00");
        }

        BigDecimal descontoCupom = new BigDecimal("0.00");
        if (requisicao.getCupom() != null && !requisicao.getCupom().isEmpty()) {
            TipoCupom tipoCupom = TipoCupom.fromString(requisicao.getCupom());
            CupomStrategy estrategiaCupom = CupomFactory.criar(tipoCupom);
            descontoCupom = estrategiaCupom.calcularDesconto(subtotal, frete, requisicao.getItens());
        }

        Regiao regiao = Regiao.fromString(requisicao.getRegiao());
        BigDecimal seguro = SeguroFactory.calcularSeguro(regiao, subtotal);

        return subtotal.subtract(descontoCupom).add(frete).add(seguro);
    }

    private BigDecimal calcularSubtotal(List<Item> itens) {
        return itens.stream()
            .map(item -> new BigDecimal(item.getPrecoUnitario())
                .multiply(new BigDecimal(item.getQuantidade())))
            .reduce(new BigDecimal("0.00"), BigDecimal::add);
    }

    private Double calcularPesoTotal(List<Item> itens) {
        return itens.stream()
            .mapToDouble(item -> item.getPesoKg() * item.getQuantidade())
            .sum();
    }
}
