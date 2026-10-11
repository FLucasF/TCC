package com.loja.checkout;

import com.loja.checkout.clube.BeneficiosClube;
import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.entrega.ResultadoEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CheckoutService {

    private final Map<String, ModalidadeEntrega> modalidades;
    private final Map<String, Cupom> cupons;
    private final Map<String, NivelClube> niveis;
    private final Map<String, FormaPagamento> formasPagamento;

    public CheckoutService(
            List<ModalidadeEntrega> modalidades,
            List<Cupom> cupons,
            List<NivelClube> niveis,
            List<FormaPagamento> formasPagamento
    ) {
        this.modalidades = modalidades.stream().collect(Collectors.toMap(ModalidadeEntrega::codigo, Function.identity()));
        this.cupons = cupons.stream().collect(Collectors.toMap(Cupom::codigo, Function.identity()));
        this.niveis = niveis.stream().collect(Collectors.toMap(NivelClube::codigo, Function.identity()));
        this.formasPagamento = formasPagamento.stream().collect(Collectors.toMap(FormaPagamento::codigo, Function.identity()));
    }

    public CheckoutResponse calcular(CheckoutRequest req) {
        validarItens(req.itens());

        if (req.nivelClube() == null || !niveis.containsKey(req.nivelClube()))
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");

        if (!Seguro.regiaoValida(req.regiao()))
            throw new CheckoutException("REGIAO_INVALIDA");

        if (req.modalidadeEntrega() == null || !modalidades.containsKey(req.modalidadeEntrega()))
            throw new CheckoutException("MODALIDADE_INVALIDA");

        BigDecimal subtotalProdutos = calcularSubtotal(req.itens());
        BigDecimal pesoTotal = calcularPeso(req.itens());

        ModalidadeEntrega modalidade = modalidades.get(req.modalidadeEntrega());
        if (!modalidade.disponivel(pesoTotal))
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");

        String cupomCodigo = req.cupom();
        Cupom cupom = null;
        if (cupomCodigo != null && !cupomCodigo.isBlank()) {
            if (!cupons.containsKey(cupomCodigo))
                throw new CheckoutException("CUPOM_INVALIDO");
            cupom = cupons.get(cupomCodigo);
            if (!cupom.aplicavel(subtotalProdutos, req.itens()))
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }

        if (req.formaPagamento() == null || !formasPagamento.containsKey(req.formaPagamento()))
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");

        int parcelas = req.parcelas() != null ? req.parcelas() : 1;
        FormaPagamento forma = formasPagamento.get(req.formaPagamento());

        if (!forma.parcelamentoValido(parcelas))
            throw new CheckoutException("PARCELAMENTO_INVALIDO");

        NivelClube nivel = niveis.get(req.nivelClube());
        BeneficiosClube beneficios = nivel.calcular(subtotalProdutos);

        ResultadoEntrega entrega = modalidade.calcular(pesoTotal);
        BigDecimal frete = beneficios.freteGratis()
                ? BigDecimal.ZERO.setScale(2)
                : entrega.frete();

        BigDecimal descontoCupom = cupom != null
                ? cupom.calcularDesconto(subtotalProdutos, frete, req.itens())
                : BigDecimal.ZERO.setScale(2);

        BigDecimal seguro = Seguro.calcular(req.regiao(), subtotalProdutos);

        BigDecimal totalPedido = Arredondamento.centavos(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro)
        );

        if (!forma.disponivel(totalPedido, parcelas))
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");

        ResultadoPagamento pagamento = forma.calcular(totalPedido, parcelas);
        BigDecimal ajuste = Arredondamento.centavos(pagamento.totalFinal().subtract(totalPedido));

        return new CheckoutResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                entrega.prazoDias(),
                seguro,
                ajuste,
                pagamento.totalFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela(),
                beneficios.creditoProximaCompra(),
                beneficios.brinde()
        );
    }

    private void validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty())
            throw new CheckoutException("PEDIDO_INVALIDO");
        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0)
                throw new CheckoutException("PEDIDO_INVALIDO");
        }
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(
                    Arredondamento.centavos(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())))
            );
        }
        return subtotal;
    }

    private BigDecimal calcularPeso(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }
}
