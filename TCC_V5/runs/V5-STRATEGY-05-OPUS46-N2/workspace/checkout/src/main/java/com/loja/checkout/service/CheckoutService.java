package com.loja.checkout.service;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
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
    private final Map<String, NivelClube> niveisClube;

    public CheckoutService(List<ModalidadeEntrega> modalidades, List<Cupom> cupons, List<NivelClube> niveisClube) {
        this.modalidades = modalidades.stream().collect(Collectors.toMap(ModalidadeEntrega::codigo, Function.identity()));
        this.cupons = cupons.stream().collect(Collectors.toMap(Cupom::codigo, Function.identity()));
        this.niveisClube = niveisClube.stream().collect(Collectors.toMap(NivelClube::codigo, Function.identity()));
    }

    public ResumoResponse calcular(PedidoRequest pedido) {
        validarItens(pedido.itens());

        NivelClube nivel = resolverNivelClube(pedido.nivelClube());
        Regiao regiao = resolverRegiao(pedido.regiao());

        BigDecimal pesoTotal = calcularPesoTotal(pedido.itens());
        ModalidadeEntrega modalidade = resolverModalidade(pedido.modalidadeEntrega(), pesoTotal);

        BigDecimal subtotal = calcularSubtotal(pedido.itens());

        Cupom cupom = resolverCupom(pedido.cupom(), pedido.itens(), subtotal);

        int parcelas = pedido.parcelas() != null ? pedido.parcelas() : 1;
        FormaPagamento formaPagamento = resolverFormaPagamento(pedido.formaPagamento(), parcelas);

        BigDecimal frete = nivel.freteGratis()
                ? BigDecimal.ZERO.setScale(2)
                : modalidade.calcularFrete(pesoTotal);

        BigDecimal descontoCupom = cupom != null
                ? cupom.calcularDesconto(pedido.itens(), subtotal, frete)
                : BigDecimal.ZERO.setScale(2);

        BigDecimal seguro = subtotal.multiply(regiao.taxaSeguro()).setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        validarDisponibilidadePagamento(formaPagamento, totalPedido);

        ResultadoPagamento resultado = formaPagamento.calcular(totalPedido, parcelas);

        BigDecimal credito = subtotal.multiply(nivel.taxaCashback()).setScale(2, RoundingMode.HALF_EVEN);

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                resultado.ajuste(),
                resultado.totalFinal(),
                resultado.parcelas(),
                resultado.valorParcela(),
                credito,
                nivel.brinde(subtotal)
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

    private NivelClube resolverNivelClube(String codigo) {
        if (codigo == null || !niveisClube.containsKey(codigo)) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
        return niveisClube.get(codigo);
    }

    private Regiao resolverRegiao(String codigo) {
        if (codigo == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private ModalidadeEntrega resolverModalidade(String codigo, BigDecimal pesoTotal) {
        if (codigo == null || !modalidades.containsKey(codigo)) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        ModalidadeEntrega modalidade = modalidades.get(codigo);
        if (!modalidade.disponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
        return modalidade;
    }

    private Cupom resolverCupom(String codigo, List<ItemRequest> itens, BigDecimal subtotal) {
        if (codigo == null) {
            return null;
        }
        if (!cupons.containsKey(codigo)) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
        Cupom cupom = cupons.get(codigo);
        if (!cupom.aplicavel(itens, subtotal)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
        return cupom;
    }

    private FormaPagamento resolverFormaPagamento(String codigo, int parcelas) {
        if (codigo == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        FormaPagamento forma;
        try {
            forma = FormaPagamento.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        if (!forma.parcelamentoValido(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
        return forma;
    }

    private void validarDisponibilidadePagamento(FormaPagamento forma, BigDecimal totalPedido) {
        if (!forma.disponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(new BigDecimal(item.quantidade())));
        }
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(new BigDecimal(item.quantidade())));
        }
        return peso;
    }
}
