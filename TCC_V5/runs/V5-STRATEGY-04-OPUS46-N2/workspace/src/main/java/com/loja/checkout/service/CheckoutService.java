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
    private final Map<String, FormaPagamento> formasPagamento;

    public CheckoutService(List<ModalidadeEntrega> modalidades,
                           List<Cupom> cupons,
                           List<NivelClube> niveisClube,
                           List<FormaPagamento> formasPagamento) {
        this.modalidades = modalidades.stream().collect(Collectors.toMap(ModalidadeEntrega::getCodigo, Function.identity()));
        this.cupons = cupons.stream().collect(Collectors.toMap(Cupom::getCodigo, Function.identity()));
        this.niveisClube = niveisClube.stream().collect(Collectors.toMap(NivelClube::getCodigo, Function.identity()));
        this.formasPagamento = formasPagamento.stream().collect(Collectors.toMap(FormaPagamento::getCodigo, Function.identity()));
    }

    public ResumoResponse calcular(PedidoRequest pedido) {
        validarItens(pedido.itens());

        BigDecimal subtotal = calcularSubtotal(pedido.itens());
        BigDecimal pesoTotal = calcularPesoTotal(pedido.itens());

        NivelClube nivel = buscar(niveisClube, pedido.nivelClube(), "NIVEL_CLUBE_INVALIDO");

        Regiao regiao = parseRegiao(pedido.regiao());

        ModalidadeEntrega modalidade = buscar(modalidades, pedido.modalidadeEntrega(), "MODALIDADE_INVALIDA");
        if (!modalidade.isDisponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal frete = modalidade.calcularFrete(pesoTotal);
        if (nivel.isFreteGratis()) {
            frete = BigDecimal.ZERO.setScale(2);
        }

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (pedido.cupom() != null && !pedido.cupom().isBlank()) {
            Cupom cupom = buscar(cupons, pedido.cupom(), "CUPOM_INVALIDO");
            if (!cupom.isAplicavel(subtotal, pedido.itens())) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = cupom.calcularDesconto(subtotal, pedido.itens(), frete);
        }

        FormaPagamento forma = buscar(formasPagamento, pedido.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");

        int parcelas = pedido.parcelas() != null ? pedido.parcelas() : 1;
        if (!forma.aceitaParcelas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal seguro = regiao.calcularSeguro(subtotal);

        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        if (!forma.isDisponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultado = forma.calcular(totalPedido, parcelas);
        BigDecimal ajuste = resultado.totalFinal().subtract(totalPedido).setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal credito = nivel.calcularCredito(subtotal);
        boolean brinde = nivel.temBrinde(subtotal);

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.getPrazoDias(),
                seguro,
                ajuste,
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

    private Regiao parseRegiao(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private <T> T buscar(Map<String, T> mapa, String codigo, String codigoErro) {
        if (codigo == null || codigo.isBlank()) {
            throw new CheckoutException(codigoErro);
        }
        T valor = mapa.get(codigo);
        if (valor == null) {
            throw new CheckoutException(codigoErro);
        }
        return valor;
    }
}
