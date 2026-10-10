package com.loja.checkout;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
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
    private final Map<String, FormaPagamento> formasPagamento;
    private final Map<String, NivelClube> niveisClube;

    public CheckoutService(List<ModalidadeEntrega> modalidades,
                           List<Cupom> cupons,
                           List<FormaPagamento> formasPagamento,
                           List<NivelClube> niveisClube) {
        this.modalidades = modalidades.stream()
                .collect(Collectors.toMap(ModalidadeEntrega::codigo, Function.identity()));
        this.cupons = cupons.stream()
                .collect(Collectors.toMap(Cupom::codigo, Function.identity()));
        this.formasPagamento = formasPagamento.stream()
                .collect(Collectors.toMap(FormaPagamento::codigo, Function.identity()));
        this.niveisClube = niveisClube.stream()
                .collect(Collectors.toMap(NivelClube::codigo, Function.identity()));
    }

    public CheckoutResponse calcular(CheckoutRequest request) {
        validarItens(request.itens());

        NivelClube nivel = resolver(niveisClube, request.nivelClube(), "NIVEL_CLUBE_INVALIDO");

        resolverRegiao(request.regiao());
        Regiao regiao = Regiao.valueOf(request.regiao());

        BigDecimal subtotal = calcularSubtotal(request.itens());
        BigDecimal pesoTotal = calcularPeso(request.itens());

        ModalidadeEntrega modalidade = resolver(modalidades, request.modalidadeEntrega(), "MODALIDADE_INVALIDA");
        if (!modalidade.disponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal frete = modalidade.calcularFrete(pesoTotal);
        int prazo = modalidade.prazoDias();

        if (nivel.freteGratis()) {
            frete = BigDecimal.ZERO.setScale(2);
        }

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (request.cupom() != null && !request.cupom().isBlank()) {
            Cupom cupom = resolver(cupons, request.cupom(), "CUPOM_INVALIDO");
            if (!cupom.aplicavel(subtotal, request.itens())) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = cupom.calcularDesconto(subtotal, request.itens(), frete);
        }

        BigDecimal seguro = subtotal.multiply(regiao.percentualSeguro())
                .setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        int parcelas = request.parcelas() != null ? request.parcelas() : 1;

        FormaPagamento pagamento = resolver(formasPagamento, request.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        if (!pagamento.parcelasPermitidas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
        if (!pagamento.disponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);

        BigDecimal credito = subtotal.multiply(nivel.percentualCredito())
                .setScale(2, RoundingMode.HALF_EVEN);

        boolean brinde = nivel.brinde(subtotal);

        return new CheckoutResponse(
                subtotal,
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
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private void resolverRegiao(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        try {
            Regiao.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private <T> T resolver(Map<String, T> mapa, String codigo, String codigoErro) {
        if (codigo == null || codigo.isBlank()) {
            throw new CheckoutException(codigoErro);
        }
        T valor = mapa.get(codigo);
        if (valor == null) {
            throw new CheckoutException(codigoErro);
        }
        return valor;
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(new BigDecimal(item.quantidade())));
        }
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularPeso(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(new BigDecimal(item.quantidade())));
        }
        return peso;
    }
}
