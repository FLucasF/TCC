package com.loja.checkout.service;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.modalidade.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.regiao.Regiao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ResumoService {

    public ResumoResponse calcular(ResumoRequest request) {
        validarItens(request.itens());
        NivelClube nivelClube = parseObrigatorio(NivelClube.class, request.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = parseObrigatorio(Regiao.class, request.regiao(), "REGIAO_INVALIDA");
        ModalidadeEntrega modalidade = parseObrigatorio(ModalidadeEntrega.class, request.modalidadeEntrega(), "MODALIDADE_INVALIDA");

        BigDecimal pesoTotal = pesoTotal(request.itens());
        if (!modalidade.disponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        Cupom cupom = null;
        BigDecimal subtotalProdutos = subtotalProdutos(request.itens());
        if (request.cupom() != null) {
            cupom = parseObrigatorio(Cupom.class, request.cupom(), "CUPOM_INVALIDO");
            if (!cupom.aplicavel(request.itens(), subtotalProdutos)) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
        }

        FormaPagamento formaPagamento = parseObrigatorio(FormaPagamento.class, request.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal frete = nivelClube.isentaFrete()
                ? Dinheiro.zero()
                : modalidade.frete(pesoTotal);

        BigDecimal descontoCupom = cupom == null
                ? Dinheiro.zero()
                : cupom.desconto(request.itens(), subtotalProdutos, frete);

        BigDecimal seguro = Dinheiro.arredondar(subtotalProdutos.multiply(regiao.percentualSeguro()));

        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        if (!formaPagamento.disponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        BigDecimal totalFinal = formaPagamento.totalFinal(totalPedido, parcelas);
        BigDecimal ajustePagamento = Dinheiro.arredondar(totalFinal.subtract(totalPedido));
        BigDecimal valorParcela = Dinheiro.arredondar(
                totalFinal.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN));

        BigDecimal creditoProximaCompra = nivelClube.credito(subtotalProdutos);
        boolean brinde = nivelClube.temBrinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajustePagamento,
                totalFinal,
                parcelas,
                valorParcela,
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

    private BigDecimal subtotalProdutos(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return Dinheiro.arredondar(subtotal);
    }

    private BigDecimal pesoTotal(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }

    private <T extends Enum<T>> T parseObrigatorio(Class<T> tipo, String valor, String codigoErro) {
        if (valor == null) {
            throw new CheckoutException(codigoErro);
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(codigoErro);
        }
    }
}
