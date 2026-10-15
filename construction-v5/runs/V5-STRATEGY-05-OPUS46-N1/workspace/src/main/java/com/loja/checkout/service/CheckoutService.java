package com.loja.checkout.service;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.FormaPagamento.ResultadoPagamento;
import com.loja.checkout.regiao.Regiao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CheckoutService {

    private final Map<String, ModalidadeEntrega> modalidades;
    private final Map<String, Cupom> cupons;
    private final Map<String, NivelClube> niveis;

    public CheckoutService(List<ModalidadeEntrega> modalidades,
                           List<Cupom> cupons,
                           List<NivelClube> niveis) {
        this.modalidades = modalidades.stream()
                .collect(Collectors.toMap(ModalidadeEntrega::codigo, Function.identity()));
        this.cupons = cupons.stream()
                .collect(Collectors.toMap(Cupom::codigo, Function.identity()));
        this.niveis = niveis.stream()
                .collect(Collectors.toMap(NivelClube::codigo, Function.identity()));
    }

    public ResumoResponse calcular(PedidoRequest pedido) {
        validarItens(pedido.itens());

        BigDecimal subtotal = calcularSubtotal(pedido.itens());
        BigDecimal pesoTotal = calcularPeso(pedido.itens());

        NivelClube nivel = niveis.get(pedido.nivelClube());
        if (nivel == null) throw new CheckoutException("NIVEL_CLUBE_INVALIDO");

        Regiao regiao = Regiao.fromCodigo(pedido.regiao());
        if (regiao == null) throw new CheckoutException("REGIAO_INVALIDA");

        ModalidadeEntrega modalidade = modalidades.get(pedido.modalidadeEntrega());
        if (modalidade == null) throw new CheckoutException("MODALIDADE_INVALIDA");
        if (!modalidade.disponivel(pesoTotal)) throw new CheckoutException("MODALIDADE_INDISPONIVEL");

        Cupom cupom = null;
        if (pedido.cupom() != null && !pedido.cupom().isBlank()) {
            cupom = cupons.get(pedido.cupom());
            if (cupom == null) throw new CheckoutException("CUPOM_INVALIDO");
            if (!cupom.aplicavel(pedido.itens(), subtotal))
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }

        FormaPagamento formaPagamento = FormaPagamento.fromCodigo(pedido.formaPagamento());
        if (formaPagamento == null) throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");

        int parcelas = pedido.parcelas() != null ? pedido.parcelas() : 1;
        if (!formaPagamento.parcelasValidas(parcelas))
            throw new CheckoutException("PARCELAMENTO_INVALIDO");

        BigDecimal frete = modalidade.calcularFrete(pesoTotal);
        if (nivel.freteGratis()) {
            frete = BigDecimal.ZERO.setScale(2);
        }

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (cupom != null) {
            descontoCupom = cupom.calcularDesconto(pedido.itens(), subtotal, frete);
        }

        BigDecimal seguro = arredondar(subtotal.multiply(regiao.taxaSeguro()));

        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        if (!formaPagamento.disponivel(totalPedido))
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");

        ResultadoPagamento resultado = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal ajuste = resultado.totalFinal().subtract(totalPedido);

        BigDecimal credito = arredondar(subtotal.multiply(nivel.percentualCredito()));
        boolean brinde = nivel.brinde(subtotal);

        return new ResumoResponse(
                subtotal, descontoCupom, frete, modalidade.prazoDias(),
                seguro, ajuste, resultado.totalFinal(), resultado.parcelas(),
                resultado.valorParcela(), credito, brinde);
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

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(
                    item.precoUnitario().multiply(new BigDecimal(item.quantidade())));
        }
        return arredondar(subtotal);
    }

    private BigDecimal calcularPeso(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(
                    item.pesoKg().multiply(new BigDecimal(item.quantidade())));
        }
        return peso;
    }

    private static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
