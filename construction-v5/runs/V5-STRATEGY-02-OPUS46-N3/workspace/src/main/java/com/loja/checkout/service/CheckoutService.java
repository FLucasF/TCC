package com.loja.checkout.service;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.clube.NiveisClubeRegistry;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.CupomRegistry;
import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.entrega.EntregaRegistry;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.PagamentoRegistry;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.regiao.Regiao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutService {

    private final EntregaRegistry entregas;
    private final CupomRegistry cupons;
    private final PagamentoRegistry pagamentos;
    private final NiveisClubeRegistry niveisClube;

    public CheckoutService(EntregaRegistry entregas, CupomRegistry cupons,
                           PagamentoRegistry pagamentos, NiveisClubeRegistry niveisClube) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.pagamentos = pagamentos;
        this.niveisClube = niveisClube;
    }

    public CheckoutResponse calcular(CheckoutRequest request) {
        validarItens(request.itens());

        NivelClube nivel = niveisClube.buscar(request.nivelClube());
        if (nivel == null) throw new CheckoutException("NIVEL_CLUBE_INVALIDO");

        Regiao regiao = Regiao.porNome(request.regiao());
        if (regiao == null) throw new CheckoutException("REGIAO_INVALIDA");

        ModalidadeEntrega modalidade = entregas.buscar(request.modalidadeEntrega());
        if (modalidade == null) throw new CheckoutException("MODALIDADE_INVALIDA");

        BigDecimal pesoTotal = calcularPesoTotal(request.itens());
        if (!modalidade.disponivel(pesoTotal)) throw new CheckoutException("MODALIDADE_INDISPONIVEL");

        Cupom cupom = null;
        if (request.cupom() != null && !request.cupom().isEmpty()) {
            cupom = cupons.buscar(request.cupom());
            if (cupom == null) throw new CheckoutException("CUPOM_INVALIDO");
        }

        BigDecimal subtotal = calcularSubtotal(request.itens());

        if (cupom != null && !cupom.aplicavel(subtotal, request.itens())) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }

        FormaPagamento formaPgto = pagamentos.buscar(request.formaPagamento());
        if (formaPgto == null) throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");

        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        if (!formaPgto.parcelasValidas(parcelas)) throw new CheckoutException("PARCELAMENTO_INVALIDO");

        BigDecimal frete = modalidade.calcularFrete(pesoTotal);
        if (nivel.freteGratis()) {
            frete = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        }

        BigDecimal descontoCupom = cupom != null
                ? cupom.calcularDesconto(subtotal, request.itens(), frete)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal seguro = subtotal.multiply(regiao.taxaSeguro()).setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal totalPedido = subtotal
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro)
                .setScale(2, RoundingMode.HALF_EVEN);

        if (!formaPgto.disponivel(totalPedido)) throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");

        ResultadoPagamento resultado = formaPgto.calcular(totalPedido, parcelas);
        BigDecimal ajuste = resultado.totalFinal().subtract(totalPedido);

        BigDecimal credito = nivel.calcularCredito(subtotal);
        boolean brinde = nivel.temBrinde(subtotal);

        return new CheckoutResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajuste,
                resultado.totalFinal(),
                resultado.parcelas(),
                resultado.valorParcela(),
                credito,
                brinde
        );
    }

    private void validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) throw new CheckoutException("PEDIDO_INVALIDO");
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
                    item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
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
