package com.loja.checkout.service;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.model.Cupom;
import com.loja.checkout.model.FormaPagamento;
import com.loja.checkout.model.ModalidadeEntrega;
import com.loja.checkout.model.NivelClube;
import com.loja.checkout.model.Regiao;
import com.loja.checkout.model.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutService {

    public ResumoResponse calcularResumo(ResumoRequest request) {
        validarItens(request.itens());
        NivelClube nivelClube = validarNivelClube(request.nivelClube());
        Regiao regiao = validarRegiao(request.regiao());
        ModalidadeEntrega modalidade = validarModalidade(request.modalidadeEntrega());

        BigDecimal pesoTotal = calcularPesoTotal(request.itens());
        if (!modalidade.disponivelPara(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        Cupom cupom = validarCupom(request.cupom());

        BigDecimal subtotalProdutos = Dinheiro.arredondar(calcularSubtotal(request.itens()));

        if (cupom != null && !cupom.aplicavel(request.itens(), subtotalProdutos)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }

        FormaPagamento formaPagamento = validarFormaPagamento(request.formaPagamento());
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal freteBase = Dinheiro.arredondar(modalidade.calcularFrete(pesoTotal));
        BigDecimal frete = nivelClube.freteGratis() ? BigDecimal.ZERO.setScale(2) : freteBase;

        BigDecimal descontoCupom = cupom == null
                ? BigDecimal.ZERO.setScale(2)
                : Dinheiro.arredondar(cupom.calcularDesconto(request.itens(), subtotalProdutos, frete));

        BigDecimal seguro = Dinheiro.arredondar(subtotalProdutos.multiply(regiao.percentualSeguro()));

        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        if (!formaPagamento.disponivelPara(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedido, parcelas);

        BigDecimal creditoProximaCompra = Dinheiro.arredondar(
                subtotalProdutos.multiply(nivelClube.percentualCredito()));
        boolean brinde = nivelClube.elegivelBrinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoEntregaDias(),
                seguro,
                resultadoPagamento.ajuste(),
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela(),
                creditoProximaCompra,
                brinde
        );
    }

    private void validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private NivelClube validarNivelClube(String nivelClube) {
        if (nivelClube == null) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
        try {
            return NivelClube.valueOf(nivelClube);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
    }

    private Regiao validarRegiao(String regiao) {
        if (regiao == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(regiao);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private ModalidadeEntrega validarModalidade(String modalidadeEntrega) {
        if (modalidadeEntrega == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        try {
            return ModalidadeEntrega.valueOf(modalidadeEntrega);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
    }

    private Cupom validarCupom(String cupom) {
        if (cupom == null) {
            return null;
        }
        try {
            return Cupom.valueOf(cupom);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
    }

    private FormaPagamento validarFormaPagamento(String formaPagamento) {
        if (formaPagamento == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        try {
            return FormaPagamento.valueOf(formaPagamento);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            total = total.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return total;
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return total;
    }
}
