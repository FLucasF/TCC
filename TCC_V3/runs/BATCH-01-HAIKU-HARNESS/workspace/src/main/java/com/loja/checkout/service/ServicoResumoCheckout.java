package com.loja.checkout.service;

import com.loja.checkout.domain.*;
import com.loja.checkout.exception.ErroCheckout;
import java.util.List;

public class ServicoResumoCheckout {

    public RespostaResumo calcularResumo(RequisicaoResumo requisicao) {
        validarRequisicao(requisicao);

        List<ItemRequisicao> itens = requisicao.getItens();
        double subtotalProdutos = calcularSubtotalProdutos(itens);
        subtotalProdutos = Arredondamento.arredondarParaCentavos(subtotalProdutos);

        ModalidadeEntrega entrega = obterEntrega(requisicao.getModalidadeEntrega());
        double pesoTotal = calcularPesoTotal(itens);
        validarDisponibilidadeEntrega(entrega, pesoTotal);

        double descontoCupom = 0.0;
        if (requisicao.getCupom() != null && !requisicao.getCupom().trim().isEmpty()) {
            Cupom cupom = obterCupom(requisicao.getCupom());
            validarAplicabilidadeCupom(cupom, subtotalProdutos);
            double freteTentativo = Arredondamento.arredondarParaCentavos(entrega.calcularValor(pesoTotal));
            descontoCupom = cupom.calcularDesconto(subtotalProdutos, freteTentativo, itens);
            descontoCupom = Arredondamento.arredondarParaCentavos(descontoCupom);
        }

        double frete = Arredondamento.arredondarParaCentavos(entrega.calcularValor(pesoTotal));
        int prazoEntregaDias = entrega.obterPrazo();

        double totalPedido = subtotalProdutos - descontoCupom + frete;
        totalPedido = Arredondamento.arredondarParaCentavos(totalPedido);

        FormaPagamento formaPagamento = obterFormaPagamento(requisicao.getFormaPagamento());
        int parcelas = requisicao.getParcelas() != null ? requisicao.getParcelas() : 1;
        validarFormaPagamento(formaPagamento, totalPedido, parcelas);

        double ajustePagamento = formaPagamento.calcularAjuste(totalPedido, parcelas);
        ajustePagamento = Arredondamento.arredondarParaCentavos(ajustePagamento);

        double totalFinal = totalPedido + ajustePagamento;
        totalFinal = Arredondamento.arredondarParaCentavos(totalFinal);

        double valorParcela = calcularValorParcela(formaPagamento, totalFinal, parcelas);
        valorParcela = Arredondamento.arredondarParaCentavos(valorParcela);

        return new RespostaResumo(
            subtotalProdutos,
            descontoCupom,
            frete,
            prazoEntregaDias,
            ajustePagamento,
            totalFinal,
            parcelas,
            valorParcela
        );
    }

    private void validarRequisicao(RequisicaoResumo requisicao) {
        if (requisicao.getItens() == null || requisicao.getItens().isEmpty()) {
            throw new ErroCheckout("PEDIDO_INVALIDO");
        }

        for (ItemRequisicao item : requisicao.getItens()) {
            if (item.getPrecoUnitario() <= 0 || item.getQuantidade() <= 0 || item.getPesoKg() < 0) {
                throw new ErroCheckout("PEDIDO_INVALIDO");
            }
        }
    }

    private double calcularSubtotalProdutos(List<ItemRequisicao> itens) {
        double subtotal = 0.0;
        for (ItemRequisicao item : itens) {
            subtotal += item.getPrecoUnitario() * item.getQuantidade();
        }
        return subtotal;
    }

    private double calcularPesoTotal(List<ItemRequisicao> itens) {
        double pesoTotal = 0.0;
        for (ItemRequisicao item : itens) {
            pesoTotal += item.getPesoKg() * item.getQuantidade();
        }
        return pesoTotal;
    }

    private ModalidadeEntrega obterEntrega(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new ErroCheckout("MODALIDADE_INVALIDA");
        }
        if (!RepositorioEntregas.existe(codigo)) {
            throw new ErroCheckout("MODALIDADE_INVALIDA");
        }
        return RepositorioEntregas.obter(codigo).get();
    }

    private void validarDisponibilidadeEntrega(ModalidadeEntrega entrega, double pesoTotal) {
        if (!entrega.estaDisponivel(pesoTotal)) {
            throw new ErroCheckout("MODALIDADE_INDISPONIVEL");
        }
    }

    private Cupom obterCupom(String codigo) {
        if (!RepositorioCupons.existe(codigo)) {
            throw new ErroCheckout("CUPOM_INVALIDO");
        }
        return RepositorioCupons.obter(codigo).get();
    }

    private void validarAplicabilidadeCupom(Cupom cupom, double subtotalProdutos) {
        if (!cupom.eAplicavel(subtotalProdutos)) {
            throw new ErroCheckout("CUPOM_NAO_APLICAVEL");
        }
    }

    private FormaPagamento obterFormaPagamento(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INVALIDA");
        }
        if (!RepositorioFormasPagamento.existe(codigo)) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INVALIDA");
        }
        return RepositorioFormasPagamento.obter(codigo).get();
    }

    private void validarFormaPagamento(FormaPagamento formaPagamento, double total, int parcelas) {
        if (parcelas < 1 || parcelas > formaPagamento.obterParcelasMaximas()) {
            throw new ErroCheckout("PARCELAMENTO_INVALIDO");
        }
        if (!formaPagamento.estaDisponivel(total, parcelas)) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private double calcularValorParcela(FormaPagamento formaPagamento, double totalFinal, int parcelas) {
        return totalFinal / parcelas;
    }
}
