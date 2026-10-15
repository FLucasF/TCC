package com.loja.checkout;

import com.loja.checkout.api.CheckoutException;
import com.loja.checkout.api.ErroCheckout;
import com.loja.checkout.api.dto.ItemRequest;
import com.loja.checkout.api.dto.ResumoRequest;
import com.loja.checkout.api.dto.ResumoResponse;
import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutService {

    public ResumoResponse calcularResumo(ResumoRequest request) {
        List<Item> itens = validarItens(request.itens());
        ModalidadeEntrega modalidade = validarModalidade(request.modalidadeEntrega());
        BigDecimal pesoTotal = pesoTotal(itens);
        validarModalidadeDisponivel(modalidade, pesoTotal);
        BigDecimal subtotalProdutos = subtotalProdutos(itens);
        BigDecimal frete = Dinheiro.arredondar(modalidade.calcularFrete(pesoTotal));
        ContextoCupom contextoCupom = new ContextoCupom(itens, subtotalProdutos, frete);
        Cupom cupom = validarCupom(request.cupom());
        validarCupomAplicavel(cupom, contextoCupom);
        FormaPagamento formaPagamento = validarFormaPagamento(request.formaPagamento());
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        validarParcelas(formaPagamento, parcelas);

        BigDecimal descontoCupom = cupom == null
                ? BigDecimal.ZERO.setScale(2)
                : cupom.calcularDesconto(contextoCupom);

        BigDecimal totalPedido = subtotalProdutos.subtract(descontoCupom).add(frete);
        validarFormaPagamentoDisponivel(formaPagamento, totalPedido);

        ResultadoPagamento resultado = formaPagamento.calcular(totalPedido, parcelas);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                resultado.ajuste(),
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela()
        );
    }

    private List<Item> validarItens(List<ItemRequest> itensRequest) {
        if (itensRequest == null || itensRequest.isEmpty()) {
            throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
        }
        return itensRequest.stream()
                .map(this::validarItem)
                .toList();
    }

    private Item validarItem(ItemRequest itemRequest) {
        if (itemRequest.precoUnitario() == null || itemRequest.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                || itemRequest.quantidade() == null || itemRequest.quantidade() <= 0
                || itemRequest.pesoKg() == null || itemRequest.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
        }
        return new Item(itemRequest.nome(), itemRequest.precoUnitario(), itemRequest.quantidade(), itemRequest.pesoKg());
    }

    private ModalidadeEntrega validarModalidade(String modalidadeEntrega) {
        if (modalidadeEntrega == null) {
            throw new CheckoutException(ErroCheckout.MODALIDADE_INVALIDA);
        }
        try {
            return ModalidadeEntrega.valueOf(modalidadeEntrega);
        } catch (IllegalArgumentException ex) {
            throw new CheckoutException(ErroCheckout.MODALIDADE_INVALIDA);
        }
    }

    private void validarModalidadeDisponivel(ModalidadeEntrega modalidade, BigDecimal pesoTotal) {
        if (!modalidade.disponivel(pesoTotal)) {
            throw new CheckoutException(ErroCheckout.MODALIDADE_INDISPONIVEL);
        }
    }

    private Cupom validarCupom(String codigoCupom) {
        if (codigoCupom == null) {
            return null;
        }
        try {
            return Cupom.valueOf(codigoCupom);
        } catch (IllegalArgumentException ex) {
            throw new CheckoutException(ErroCheckout.CUPOM_INVALIDO);
        }
    }

    private void validarCupomAplicavel(Cupom cupom, ContextoCupom contexto) {
        if (cupom != null && !cupom.aplicavel(contexto)) {
            throw new CheckoutException(ErroCheckout.CUPOM_NAO_APLICAVEL);
        }
    }

    private FormaPagamento validarFormaPagamento(String formaPagamento) {
        if (formaPagamento == null) {
            throw new CheckoutException(ErroCheckout.FORMA_PAGAMENTO_INVALIDA);
        }
        try {
            return FormaPagamento.valueOf(formaPagamento);
        } catch (IllegalArgumentException ex) {
            throw new CheckoutException(ErroCheckout.FORMA_PAGAMENTO_INVALIDA);
        }
    }

    private void validarParcelas(FormaPagamento formaPagamento, int parcelas) {
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException(ErroCheckout.PARCELAMENTO_INVALIDO);
        }
    }

    private void validarFormaPagamentoDisponivel(FormaPagamento formaPagamento, BigDecimal totalPedido) {
        if (!formaPagamento.disponivel(totalPedido)) {
            throw new CheckoutException(ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
        }
    }

    private BigDecimal subtotalProdutos(List<Item> itens) {
        return Dinheiro.arredondar(itens.stream()
                .map(Item::precoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal pesoTotal(List<Item> itens) {
        return itens.stream()
                .map(Item::pesoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
