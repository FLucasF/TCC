package com.loja.checkout.service;

import com.loja.checkout.domain.Regiao;
import com.loja.checkout.domain.clube.NivelClube;
import com.loja.checkout.domain.cupom.Cupom;
import com.loja.checkout.domain.entrega.ModalidadeEntrega;
import com.loja.checkout.domain.pagamento.FormaPagamento;
import com.loja.checkout.domain.pagamento.ResultadoPagamento;
import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.exception.CheckoutException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
public class CheckoutService {

    private final Map<String, ModalidadeEntrega> modalidades;
    private final Map<String, Cupom> cupons;
    private final Map<String, NivelClube> niveisClube;
    private final Map<String, FormaPagamento> formasPagamento;

    public CheckoutService(Map<String, ModalidadeEntrega> modalidades,
                           Map<String, Cupom> cupons,
                           Map<String, NivelClube> niveisClube,
                           Map<String, FormaPagamento> formasPagamento) {
        this.modalidades = modalidades;
        this.cupons = cupons;
        this.niveisClube = niveisClube;
        this.formasPagamento = formasPagamento;
    }

    public CheckoutResponse calcular(CheckoutRequest request) {
        validarItens(request.itens());

        NivelClube nivel = niveisClube.get(request.nivelClube());
        if (nivel == null) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }

        Regiao regiao = Regiao.buscar(request.regiao());
        if (regiao == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }

        ModalidadeEntrega entrega = modalidades.get(request.modalidadeEntrega());
        if (entrega == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        BigDecimal pesoTotal = calcularPesoTotal(request.itens());
        if (!entrega.disponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        Cupom cupom = null;
        if (request.cupom() != null && !request.cupom().isEmpty()) {
            cupom = cupons.get(request.cupom());
            if (cupom == null) {
                throw new CheckoutException("CUPOM_INVALIDO");
            }
        }

        FormaPagamento pagamento = formasPagamento.get(request.formaPagamento());
        if (pagamento == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        if (!pagamento.parcelamentoValido(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal subtotal = calcularSubtotal(request.itens());

        BigDecimal frete = entrega.calcularFrete(pesoTotal);
        if (nivel.freteGratis()) {
            frete = BigDecimal.ZERO.setScale(2);
        }

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (cupom != null) {
            if (!cupom.aplicavel(subtotal)) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = cupom.calcularDesconto(subtotal, frete, request.itens());
        }

        BigDecimal seguro = subtotal.multiply(regiao.taxaSeguro()).setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        if (!pagamento.disponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);

        BigDecimal credito = nivel.calcularCredito(subtotal);
        boolean brinde = nivel.brinde(subtotal);

        return new CheckoutResponse(
                subtotal,
                descontoCupom,
                frete,
                entrega.prazoDias(),
                seguro,
                resultado.ajuste(),
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                credito,
                brinde);
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
            subtotal = subtotal.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }
}
