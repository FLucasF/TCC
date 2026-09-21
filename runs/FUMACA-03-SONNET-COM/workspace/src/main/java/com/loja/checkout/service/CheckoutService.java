package com.loja.checkout.service;

import com.loja.checkout.domain.Dinheiro;
import com.loja.checkout.domain.Item;
import com.loja.checkout.domain.cupom.ContextoCupom;
import com.loja.checkout.domain.cupom.Cupom;
import com.loja.checkout.domain.entrega.ModalidadeEntrega;
import com.loja.checkout.domain.pagamento.FormaPagamento;
import com.loja.checkout.domain.pagamento.ResultadoPagamento;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutService {

    public ResumoResponse calcularResumo(ResumoRequest request) {
        List<Item> itens = validarItens(request.itens());

        ModalidadeEntrega modalidade = resolverModalidade(request.modalidadeEntrega());
        BigDecimal pesoTotal = itens.stream()
                .map(Item::pesoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!modalidade.disponivelPara(pesoTotal)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotalProdutos = Dinheiro.arredondar(itens.stream()
                .map(Item::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal frete = modalidade.calcularFrete(pesoTotal);

        BigDecimal descontoCupom = resolverDescontoCupom(request.cupom(), itens, subtotalProdutos, frete);

        BigDecimal totalPedido = Dinheiro.arredondar(subtotalProdutos.subtract(descontoCupom).add(frete));

        FormaPagamento formaPagamento = resolverFormaPagamento(request.formaPagamento());
        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!formaPagamento.disponivelPara(totalPedido)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultado = formaPagamento.calcularAjuste(totalPedido, parcelas);
        BigDecimal ajustePagamento = Dinheiro.arredondar(resultado.totalFinal().subtract(totalPedido));

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                ajustePagamento,
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela());
    }

    private List<Item> validarItens(List<ItemRequest> itensRequest) {
        if (itensRequest == null || itensRequest.isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        List<Item> itens = itensRequest.stream()
                .map(item -> new Item(item.precoUnitario(), item.quantidade() != null ? item.quantidade() : 0,
                        item.pesoKg()))
                .toList();
        boolean algumInvalido = itens.stream().anyMatch(item -> !item.valido());
        if (algumInvalido) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        return itens;
    }

    private ModalidadeEntrega resolverModalidade(String modalidadeEntrega) {
        if (modalidadeEntrega == null) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INVALIDA);
        }
        try {
            return ModalidadeEntrega.valueOf(modalidadeEntrega);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INVALIDA);
        }
    }

    private BigDecimal resolverDescontoCupom(String codigoCupom, List<Item> itens, BigDecimal subtotalProdutos,
            BigDecimal frete) {
        if (codigoCupom == null || codigoCupom.isBlank()) {
            return Dinheiro.arredondar(BigDecimal.ZERO);
        }
        Cupom cupom;
        try {
            cupom = Cupom.valueOf(codigoCupom);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(CodigoErro.CUPOM_INVALIDO);
        }
        ContextoCupom contexto = new ContextoCupom(itens, subtotalProdutos, frete);
        if (!cupom.aplicavel(contexto)) {
            throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return cupom.calcularDesconto(contexto);
    }

    private FormaPagamento resolverFormaPagamento(String formaPagamento) {
        if (formaPagamento == null) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }
        try {
            return FormaPagamento.valueOf(formaPagamento);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }
    }
}
