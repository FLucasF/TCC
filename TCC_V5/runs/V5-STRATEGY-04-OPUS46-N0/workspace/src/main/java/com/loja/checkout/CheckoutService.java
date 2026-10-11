package com.loja.checkout;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
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

    private final Map<String, ModalidadeEntrega> entregaMap;
    private final Map<String, Cupom> cupomMap;
    private final Map<String, FormaPagamento> pagamentoMap;
    private final Map<String, NivelClube> clubeMap;

    public CheckoutService(List<ModalidadeEntrega> entregas,
                           List<Cupom> cupons,
                           List<FormaPagamento> pagamentos,
                           List<NivelClube> niveis) {
        this.entregaMap = entregas.stream().collect(Collectors.toMap(ModalidadeEntrega::codigo, Function.identity()));
        this.cupomMap = cupons.stream().collect(Collectors.toMap(Cupom::codigo, Function.identity()));
        this.pagamentoMap = pagamentos.stream().collect(Collectors.toMap(FormaPagamento::codigo, Function.identity()));
        this.clubeMap = niveis.stream().collect(Collectors.toMap(NivelClube::codigo, Function.identity()));
    }

    public CheckoutResponse calcular(CheckoutRequest request) {
        List<ItemRequest> itens = request.itens();
        validarItens(itens);

        String nivelClubeCode = request.nivelClube();
        if (nivelClubeCode == null || !clubeMap.containsKey(nivelClubeCode)) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
        NivelClube clube = clubeMap.get(nivelClubeCode);

        String regiaoCode = request.regiao();
        Regiao regiao = parseRegiao(regiaoCode);

        String entregaCode = request.modalidadeEntrega();
        if (entregaCode == null || !entregaMap.containsKey(entregaCode)) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        ModalidadeEntrega entrega = entregaMap.get(entregaCode);

        BigDecimal pesoTotal = calcularPesoTotal(itens);
        if (!entrega.disponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotalProdutos = calcularSubtotal(itens);

        Cupom cupom = null;
        if (request.cupom() != null && !request.cupom().isBlank()) {
            if (!cupomMap.containsKey(request.cupom())) {
                throw new CheckoutException("CUPOM_INVALIDO");
            }
            cupom = cupomMap.get(request.cupom());
            if (!cupom.aplicavel(subtotalProdutos, itens)) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
        }

        String pagamentoCode = request.formaPagamento();
        if (pagamentoCode == null || !pagamentoMap.containsKey(pagamentoCode)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        FormaPagamento pagamento = pagamentoMap.get(pagamentoCode);

        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        if (!pagamento.parcelamentoValido(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal frete = entrega.calcularFrete(pesoTotal);
        int prazo = entrega.prazoDias();

        if (clube.freteGratis()) {
            frete = BigDecimal.ZERO.setScale(2);
        }

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (cupom != null) {
            descontoCupom = cupom.calcularDesconto(subtotalProdutos, itens, frete);
        }

        BigDecimal seguro = subtotalProdutos.multiply(regiao.getTaxaSeguro())
                .setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal totalPedido = subtotalProdutos
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro)
                .setScale(2, RoundingMode.HALF_EVEN);

        if (!pagamento.disponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);

        BigDecimal credito = clube.calcularCredito(subtotalProdutos);
        boolean brinde = clube.brinde(subtotalProdutos);

        return new CheckoutResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                prazo,
                seguro,
                resultado.ajuste(),
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                credito,
                brinde
        );
    }

    private void validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            if (item.quantidade() == null || item.quantidade() <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            if (item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private Regiao parseRegiao(String code) {
        if (code == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(code);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(
                    item.precoUnitario().multiply(new BigDecimal(item.quantidade()))
            );
        }
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(
                    item.pesoKg().multiply(new BigDecimal(item.quantidade()))
            );
        }
        return peso;
    }
}
