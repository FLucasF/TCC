package com.loja.checkout.service;

import com.loja.checkout.domain.ContextoCupom;
import com.loja.checkout.domain.Cupom;
import com.loja.checkout.domain.Dinheiro;
import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.ModalidadeEntrega;
import com.loja.checkout.domain.ResultadoPagamento;
import com.loja.checkout.dto.ItemPedidoDto;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.ErroCodigo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutService {

    public ResumoResponse calcularResumo(ResumoRequest request) {
        List<ItemPedidoDto> itens = validarItens(request.itens());

        BigDecimal subtotalProdutos = Dinheiro.arredondar(calcularSubtotalProdutos(itens));
        BigDecimal pesoPedido = calcularPesoPedido(itens);

        ModalidadeEntrega modalidade = resolverModalidade(request.modalidadeEntrega());
        if (!modalidade.disponivel(pesoPedido)) {
            throw new CheckoutException(ErroCodigo.MODALIDADE_INDISPONIVEL);
        }
        BigDecimal frete = Dinheiro.arredondar(modalidade.calcularFrete(pesoPedido));

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (request.cupom() != null) {
            Cupom cupom = resolverCupom(request.cupom());
            ContextoCupom contexto = new ContextoCupom(subtotalProdutos, frete, itens);
            if (!cupom.aplicavel(contexto)) {
                throw new CheckoutException(ErroCodigo.CUPOM_NAO_APLICAVEL);
            }
            descontoCupom = Dinheiro.arredondar(cupom.calcularDesconto(contexto));
        }

        FormaPagamento formaPagamento = resolverFormaPagamento(request.formaPagamento());
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException(ErroCodigo.PARCELAMENTO_INVALIDO);
        }

        BigDecimal totalPedido = Dinheiro.arredondar(subtotalProdutos.subtract(descontoCupom).add(frete));
        if (!formaPagamento.disponivel(totalPedido)) {
            throw new CheckoutException(ErroCodigo.FORMA_PAGAMENTO_INDISPONIVEL);
        }

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

    private List<ItemPedidoDto> validarItens(List<ItemPedidoDto> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(ErroCodigo.PEDIDO_INVALIDO);
        }
        for (ItemPedidoDto item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException(ErroCodigo.PEDIDO_INVALIDO);
            }
        }
        return itens;
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemPedidoDto> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemPedidoDto item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return subtotal;
    }

    private BigDecimal calcularPesoPedido(List<ItemPedidoDto> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemPedidoDto item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }

    private ModalidadeEntrega resolverModalidade(String modalidadeEntrega) {
        if (modalidadeEntrega == null) {
            throw new CheckoutException(ErroCodigo.MODALIDADE_INVALIDA);
        }
        try {
            return ModalidadeEntrega.valueOf(modalidadeEntrega);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(ErroCodigo.MODALIDADE_INVALIDA);
        }
    }

    private Cupom resolverCupom(String cupom) {
        try {
            return Cupom.valueOf(cupom);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(ErroCodigo.CUPOM_INVALIDO);
        }
    }

    private FormaPagamento resolverFormaPagamento(String formaPagamento) {
        if (formaPagamento == null) {
            throw new CheckoutException(ErroCodigo.FORMA_PAGAMENTO_INVALIDA);
        }
        try {
            return FormaPagamento.valueOf(formaPagamento);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(ErroCodigo.FORMA_PAGAMENTO_INVALIDA);
        }
    }
}
