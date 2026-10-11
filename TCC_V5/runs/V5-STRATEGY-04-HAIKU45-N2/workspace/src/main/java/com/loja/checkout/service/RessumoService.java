package com.loja.checkout.service;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import com.loja.checkout.model.*;
import com.loja.checkout.cupom.*;
import com.loja.checkout.entrega.*;
import com.loja.checkout.pagamento.*;
import com.loja.checkout.clube.*;
import com.loja.checkout.seguro.*;
import com.loja.checkout.util.Arredonda;

@Service
public class RessumoService {
    public Object calcular(RessumoRequest request) {
        if (request.itens == null || request.itens.isEmpty()) {
            return new ErroResponse("PEDIDO_INVALIDO");
        }

        for (Item item : request.itens) {
            if (item.nome == null || item.nome.isBlank() ||
                item.precoUnitario <= 0 ||
                item.quantidade <= 0 ||
                item.pesoKg < 0) {
                return new ErroResponse("PEDIDO_INVALIDO");
            }
        }

        if (request.nivelClube == null) {
            return new ErroResponse("NIVEL_CLUBE_INVALIDO");
        }

        if (request.regiao == null) {
            return new ErroResponse("REGIAO_INVALIDA");
        }

        if (request.modalidadeEntrega == null) {
            return new ErroResponse("MODALIDADE_INVALIDA");
        }

        CalculadoraEntrega calculadoraEntrega = FabricaEntrega.criar(request.modalidadeEntrega);
        if (!calculadoraEntrega.podeEntregar(request.itens)) {
            return new ErroResponse("MODALIDADE_INDISPONIVEL");
        }

        if (request.formaPagamento == null) {
            return new ErroResponse("FORMA_PAGAMENTO_INVALIDA");
        }

        CalculadoraCupom cupom = FabricaCupom.criar(request.cupom);
        if (request.cupom != null && !request.cupom.isBlank() && cupom == null) {
            return new ErroResponse("CUPOM_INVALIDO");
        }

        if (cupom instanceof CupomMenos50) {
            BigDecimal subtotal = calcularSubtotal(request.itens);
            if (!((CupomMenos50) cupom).podeAplicar(subtotal)) {
                return new ErroResponse("CUPOM_NAO_APLICAVEL");
            }
        }

        CalculadoraPagamento calculadoraPagamento = FabricaPagamento.criar(request.formaPagamento);
        if (!calculadoraPagamento.podeParcelar(request.parcelas)) {
            return new ErroResponse("PARCELAMENTO_INVALIDO");
        }

        BigDecimal subtotalProdutos = calcularSubtotal(request.itens);
        BigDecimal descontoCupom;

        if (cupom instanceof CupomFreteGratis) {
            descontoCupom = Arredonda.arredonda(BigDecimal.ZERO);
        } else {
            descontoCupom = cupom.calcular(request.itens, subtotalProdutos);
        }

        BigDecimal frete;
        BeneficiosClube beneficiosClube = FabricaClube.criar(request.nivelClube);
        if (beneficiosClube.temFretGratis()) {
            frete = new BigDecimal("0.00");
        } else {
            frete = calculadoraEntrega.calcular(request.itens);
        }

        if (cupom instanceof CupomFreteGratis) {
            descontoCupom = Arredonda.arredonda(frete);
        }

        CalculadoraSeguro calculadoraSeguro = FabricaSeguro.criar(request.regiao);
        BigDecimal seguro = calculadoraSeguro.calcular(subtotalProdutos);

        BigDecimal totalPedido = subtotalProdutos
            .subtract(descontoCupom)
            .add(frete)
            .add(seguro);

        if (!calculadoraPagamento.podeUsar(totalPedido)) {
            return new ErroResponse("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        PagamentoInfo pagamentoInfo = calculadoraPagamento.calcular(totalPedido, request.parcelas);
        BigDecimal totalFinal = totalPedido.add(pagamentoInfo.ajuste);

        boolean temBrinde = beneficiosClube.temBrindeAcima500() &&
                            subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;

        BigDecimal creditoProximaCompra = beneficiosClube.calcularCredito(subtotalProdutos);

        RessumoResponse response = new RessumoResponse();
        response.subtotalProdutos = subtotalProdutos;
        response.descontoCupom = descontoCupom;
        response.frete = frete;
        response.prazoEntregaDias = calculadoraEntrega.getPrazo();
        response.seguro = seguro;
        response.ajustePagamento = pagamentoInfo.ajuste;
        response.totalFinal = totalFinal;
        response.parcelas = pagamentoInfo.parcelas;
        response.valorParcela = pagamentoInfo.valorParcela;
        response.creditoProximaCompra = creditoProximaCompra;
        response.brinde = temBrinde;

        return response;
    }

    private BigDecimal calcularSubtotal(List<Item> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Item item : itens) {
            BigDecimal precoItem = new BigDecimal(String.valueOf(item.precoUnitario));
            BigDecimal quantidade = new BigDecimal(item.quantidade);
            subtotal = subtotal.add(precoItem.multiply(quantidade));
        }
        return Arredonda.arredonda(subtotal);
    }
}
