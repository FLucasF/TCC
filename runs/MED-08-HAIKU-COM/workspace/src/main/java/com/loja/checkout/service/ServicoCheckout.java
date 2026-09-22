package com.loja.checkout.service;

import com.loja.checkout.api.ItemRequisicao;
import com.loja.checkout.api.RequisicaoCheckout;
import com.loja.checkout.api.RespostaCheckout;
import com.loja.checkout.domain.cupom.CupomFrteGratis;
import com.loja.checkout.domain.cupom.Cupom;
import com.loja.checkout.domain.cupom.ItemCupom;
import com.loja.checkout.domain.cupom.RegistroCupons;
import com.loja.checkout.domain.modalidade.Modalidade;
import com.loja.checkout.domain.modalidade.RegistroModalidades;
import com.loja.checkout.domain.pagamento.FormaPagamento;
import com.loja.checkout.domain.pagamento.RegistroFormasPagamento;
import com.loja.checkout.exceptions.ErroCheckout;
import com.loja.checkout.utils.Arredondamento;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class ServicoCheckout {

    public RespostaCheckout calcularResumo(RequisicaoCheckout requisicao) throws ErroCheckout {
        validarRequisicao(requisicao);

        BigDecimal subtotal = calcularSubtotal(requisicao.itens);

        Modalidade modalidade = RegistroModalidades.obter(requisicao.modalidadeEntrega);
        BigDecimal pesoTotal = calcularPesoTotal(requisicao.itens);
        BigDecimal frete = modalidade.calcularFrete(pesoTotal);
        int prazo = modalidade.obterPrazoEntrega();

        String codigoCupom = requisicao.cupom;
        BigDecimal desconto = BigDecimal.ZERO;

        if (codigoCupom != null && !codigoCupom.isBlank()) {
            validarCupom(codigoCupom, subtotal, requisicao.itens);
            Cupom cupom = RegistroCupons.obter(codigoCupom);

            if (cupom instanceof CupomFrteGratis) {
                ((CupomFrteGratis) cupom).definirFrete(frete);
            }

            desconto = cupom.calcularDesconto(subtotal, converterParaItemCupom(requisicao.itens));
        }

        BigDecimal total = subtotal.subtract(desconto).add(frete);
        total = Arredondamento.arredondarParaCentavos(total);

        int parcelas = requisicao.parcelas != null ? requisicao.parcelas : 1;
        FormaPagamento forma = RegistroFormasPagamento.obter(requisicao.formaPagamento);

        validarFormaPagamento(requisicao.formaPagamento, parcelas, total);

        BigDecimal ajuste = forma.calcularAjuste(total, parcelas);
        BigDecimal totalFinal = total.add(ajuste);
        totalFinal = Arredondamento.arredondarParaCentavos(totalFinal);

        BigDecimal valorParcela = forma.calcularValorParcela(totalFinal, parcelas);

        return new RespostaCheckout(
                subtotal,
                desconto,
                frete,
                prazo,
                ajuste,
                totalFinal,
                parcelas,
                valorParcela
        );
    }

    private void validarRequisicao(RequisicaoCheckout requisicao) throws ErroCheckout {
        if (requisicao.itens == null || requisicao.itens.isEmpty()) {
            throw new ErroCheckout("PEDIDO_INVALIDO");
        }

        for (ItemRequisicao item : requisicao.itens) {
            if (item.precoUnitario == null || item.precoUnitario.compareTo(BigDecimal.ZERO) <= 0 ||
                    item.quantidade <= 0 ||
                    item.pesoKg == null || item.pesoKg.compareTo(BigDecimal.ZERO) < 0) {
                throw new ErroCheckout("PEDIDO_INVALIDO");
            }
        }

        if (requisicao.modalidadeEntrega == null || requisicao.modalidadeEntrega.isBlank()) {
            throw new ErroCheckout("MODALIDADE_INVALIDA");
        }

        if (!RegistroModalidades.existe(requisicao.modalidadeEntrega)) {
            throw new ErroCheckout("MODALIDADE_INVALIDA");
        }

        Modalidade modalidade = RegistroModalidades.obter(requisicao.modalidadeEntrega);
        BigDecimal pesoTotal = calcularPesoTotal(requisicao.itens);
        if (!modalidade.estaDisponivel(pesoTotal)) {
            throw new ErroCheckout("MODALIDADE_INDISPONIVEL");
        }

        if (requisicao.formaPagamento == null || requisicao.formaPagamento.isBlank()) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INVALIDA");
        }

        if (!RegistroFormasPagamento.existe(requisicao.formaPagamento)) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private void validarCupom(String codigoCupom, BigDecimal subtotal, List<ItemRequisicao> itens) throws ErroCheckout {
        if (!RegistroCupons.existe(codigoCupom)) {
            throw new ErroCheckout("CUPOM_INVALIDO");
        }

        Cupom cupom = RegistroCupons.obter(codigoCupom);
        if (!cupom.podeAplicar(subtotal, converterParaItemCupom(itens))) {
            throw new ErroCheckout("CUPOM_NAO_APLICAVEL");
        }
    }

    private void validarFormaPagamento(String codigoFormaPagamento, int parcelas, BigDecimal total) throws ErroCheckout {
        int parcelasAUtilizar = parcelas > 0 ? parcelas : 1;

        if (!RegistroFormasPagamento.existe(codigoFormaPagamento)) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INVALIDA");
        }

        FormaPagamento forma = RegistroFormasPagamento.obter(codigoFormaPagamento);

        if (parcelasAUtilizar < 1 || parcelasAUtilizar > 12) {
            throw new ErroCheckout("PARCELAMENTO_INVALIDO");
        }

        if ("PIX".equals(codigoFormaPagamento) && parcelasAUtilizar != 1) {
            throw new ErroCheckout("PARCELAMENTO_INVALIDO");
        }

        if ("BOLETO".equals(codigoFormaPagamento) && parcelasAUtilizar != 1) {
            throw new ErroCheckout("PARCELAMENTO_INVALIDO");
        }

        if ("CARTAO".equals(codigoFormaPagamento) && (parcelasAUtilizar < 1 || parcelasAUtilizar > 12)) {
            throw new ErroCheckout("PARCELAMENTO_INVALIDO");
        }

        if (!forma.podeAplicar(total, parcelasAUtilizar)) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private BigDecimal calcularSubtotal(List<ItemRequisicao> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequisicao item : itens) {
            BigDecimal precoTotal = item.precoUnitario.multiply(new BigDecimal(item.quantidade));
            subtotal = subtotal.add(precoTotal);
        }
        return Arredondamento.arredondarParaCentavos(subtotal);
    }

    private BigDecimal calcularPesoTotal(List<ItemRequisicao> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ItemRequisicao item : itens) {
            BigDecimal peso = item.pesoKg.multiply(new BigDecimal(item.quantidade));
            pesoTotal = pesoTotal.add(peso);
        }
        return pesoTotal;
    }

    private List<ItemCupom> converterParaItemCupom(List<ItemRequisicao> itens) {
        return itens.stream()
                .map(item -> new ItemCupom(item.nome, item.precoUnitario, item.quantidade))
                .collect(Collectors.toList());
    }
}
