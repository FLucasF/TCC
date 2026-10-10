package com.loja.checkout.service;

import com.loja.checkout.dto.ItemDto;
import com.loja.checkout.dto.ResultadoPagamento;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.PedidoException;
import com.loja.checkout.model.Cupom;
import com.loja.checkout.model.FormaPagamento;
import com.loja.checkout.model.ModalidadeEntrega;
import com.loja.checkout.model.NivelClube;
import com.loja.checkout.model.Regiao;
import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutService {

    public ResumoResponse calcular(ResumoRequest request) {
        List<ItemDto> itens = request.itens();
        validarItens(itens);

        NivelClube nivel = parseEnum(NivelClube.class, request.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = parseEnum(Regiao.class, request.regiao(), "REGIAO_INVALIDA");
        ModalidadeEntrega modalidade = parseEnum(ModalidadeEntrega.class, request.modalidadeEntrega(), "MODALIDADE_INVALIDA");

        BigDecimal subtotalProdutos = Dinheiro.round(somaProdutos(itens));
        BigDecimal pesoTotal = somaPeso(itens);

        if (!modalidade.disponivel(pesoTotal)) {
            throw new PedidoException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal freteBase = Dinheiro.round(modalidade.custo(pesoTotal));
        BigDecimal frete = nivel.freteGratis() ? new BigDecimal("0.00") : freteBase;

        BigDecimal descontoCupom = new BigDecimal("0.00");
        if (request.cupom() != null && !request.cupom().isBlank()) {
            Cupom cupom = parseEnum(Cupom.class, request.cupom(), "CUPOM_INVALIDO");
            if (!cupom.aplicavel(itens, subtotalProdutos)) {
                throw new PedidoException("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = Dinheiro.round(cupom.desconto(itens, subtotalProdutos, frete));
        }

        FormaPagamento formaPagamento = parseEnum(FormaPagamento.class, request.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new PedidoException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal seguro = Dinheiro.round(subtotalProdutos.multiply(regiao.getPercentualSeguro()));
        BigDecimal totalPedido = Dinheiro.round(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        if (!formaPagamento.disponivel(totalPedido)) {
            throw new PedidoException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultado = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal creditoProximaCompra = Dinheiro.round(subtotalProdutos.multiply(nivel.percentualCredito()));
        boolean brinde = nivel.temBrinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                resultado.ajuste(),
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                creditoProximaCompra,
                brinde);
    }

    private void validarItens(List<ItemDto> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new PedidoException("PEDIDO_INVALIDO");
        }
        for (ItemDto item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new PedidoException("PEDIDO_INVALIDO");
            }
        }
    }

    private BigDecimal somaProdutos(List<ItemDto> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemDto item : itens) {
            total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return total;
    }

    private BigDecimal somaPeso(List<ItemDto> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemDto item : itens) {
            total = total.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return total;
    }

    private <T extends Enum<T>> T parseEnum(Class<T> tipo, String valor, String codigoErro) {
        if (valor == null || valor.isBlank()) {
            throw new PedidoException(codigoErro);
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            throw new PedidoException(codigoErro);
        }
    }
}
