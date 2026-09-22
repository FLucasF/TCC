package com.loja.checkout.service;

import com.loja.checkout.domain.Cupom;
import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.Modalidade;
import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemDto;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.util.Arredondador;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class CheckoutService {

    public CheckoutResponse calcularResumo(CheckoutRequest request) {
        validarPedido(request);

        List<ItemDto> itens = request.getItens();
        BigDecimal subtotalProdutos = calcularSubtotal(itens);

        Modalidade modalidade = Modalidade.fromCodigo(request.getModalidadeEntrega());
        validarModalidade(modalidade, request.getModalidadeEntrega(), itens);

        BigDecimal pesoTotal = calcularPesoTotal(itens);
        BigDecimal frete = Arredondador.arredondar(modalidade.calcularFrete(pesoTotal));

        String codigoCupom = request.getCupom();
        BigDecimal descontoCupom = Arredondador.arredondar(BigDecimal.ZERO);

        if (codigoCupom != null && !codigoCupom.isEmpty()) {
            validarCupom(codigoCupom, subtotalProdutos, itens);
            Cupom cupom = Cupom.fromCodigo(codigoCupom);

            if ("LEVE3PAGUE2".equals(codigoCupom)) {
                descontoCupom = calcularDescontoLeve3Pague2(itens);
            } else if ("FRETEGRATIS".equals(codigoCupom)) {
                descontoCupom = frete;
            } else {
                descontoCupom = cupom.calcularDesconto(subtotalProdutos, getQuantidades(itens), frete);
            }
            descontoCupom = Arredondador.arredondar(descontoCupom);
        }

        BigDecimal totalPedido = Arredondador.arredondar(
            subtotalProdutos.subtract(descontoCupom).add(frete)
        );

        FormaPagamento formaPagamento = FormaPagamento.fromCodigo(request.getFormaPagamento());
        validarFormaPagamento(formaPagamento, request.getFormaPagamento(), totalPedido, request.getParcelas());

        BigDecimal ajustePagamento = Arredondador.arredondar(
            formaPagamento.calcularAjuste(totalPedido, request.getParcelas())
        );

        BigDecimal totalFinal = Arredondador.arredondar(totalPedido.add(ajustePagamento));

        int parcelas = request.getParcelas();
        BigDecimal valorParcela = calcularValorParcela(totalFinal, formaPagamento, parcelas);

        return new CheckoutResponse(
            Arredondador.arredondar(subtotalProdutos),
            descontoCupom,
            frete,
            modalidade.getPrazo(),
            ajustePagamento,
            totalFinal,
            parcelas,
            valorParcela
        );
    }

    private void validarPedido(CheckoutRequest request) {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (ItemDto item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().signum() <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg().signum() < 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private void validarModalidade(Modalidade modalidade, String codigoModalidade, List<ItemDto> itens) {
        if (modalidade == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        BigDecimal pesoTotal = calcularPesoTotal(itens);
        if (!modalidade.isDisponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
    }

    private void validarCupom(String codigoCupom, BigDecimal subtotalProdutos, List<ItemDto> itens) {
        if (!Cupom.existe(codigoCupom)) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }

        Cupom cupom = Cupom.fromCodigo(codigoCupom);
        if (!cupom.isAplicavel(subtotalProdutos, getQuantidades(itens))) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
    }

    private void validarFormaPagamento(FormaPagamento formaPagamento, String codigoFormaPagamento,
                                      BigDecimal totalPedido, int parcelas) {
        if (formaPagamento == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        if (parcelas < formaPagamento.getMinParcelas() || parcelas > formaPagamento.getMaxParcelas()) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        if (!formaPagamento.isDisponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private BigDecimal calcularSubtotal(List<ItemDto> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemDto item : itens) {
            BigDecimal precoItem = item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade()));
            subtotal = subtotal.add(precoItem);
        }
        return Arredondador.arredondar(subtotal);
    }

    private BigDecimal calcularPesoTotal(List<ItemDto> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ItemDto item : itens) {
            pesoTotal = pesoTotal.add(item.getPesoKg().multiply(new BigDecimal(item.getQuantidade())));
        }
        return pesoTotal;
    }

    private BigDecimal calcularDescontoLeve3Pague2(List<ItemDto> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemDto item : itens) {
            int itensGratis = item.getQuantidade() / 3;
            BigDecimal descontoItem = item.getPrecoUnitario().multiply(new BigDecimal(itensGratis));
            desconto = desconto.add(descontoItem);
        }
        return desconto;
    }

    private List<Integer> getQuantidades(List<ItemDto> itens) {
        List<Integer> quantidades = new ArrayList<>();
        for (ItemDto item : itens) {
            quantidades.add(item.getQuantidade());
        }
        return quantidades;
    }

    private BigDecimal calcularValorParcela(BigDecimal totalFinal, FormaPagamento formaPagamento, int parcelas) {
        return Arredondador.arredondar(totalFinal.divide(new BigDecimal(parcelas), 10, RoundingMode.HALF_EVEN));
    }
}
