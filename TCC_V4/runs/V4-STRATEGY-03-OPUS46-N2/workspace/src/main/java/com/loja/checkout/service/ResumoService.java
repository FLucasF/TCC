package com.loja.checkout.service;

import com.loja.checkout.domain.clube.NivelClube;
import com.loja.checkout.domain.cupom.Cupom;
import com.loja.checkout.domain.entrega.ModalidadeEntrega;
import com.loja.checkout.domain.pagamento.FormaPagamento;
import com.loja.checkout.domain.pagamento.ResultadoPagamento;
import com.loja.checkout.domain.regiao.Regiao;
import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ResumoService {

    private final Map<String, ModalidadeEntrega> modalidades;
    private final Map<String, Cupom> cupons;
    private final Map<String, NivelClube> niveis;
    private final Map<String, FormaPagamento> formasPagamento;

    public ResumoService(List<ModalidadeEntrega> modalidades,
                         List<Cupom> cupons,
                         List<NivelClube> niveis,
                         List<FormaPagamento> formasPagamento) {
        this.modalidades = indexar(modalidades, ModalidadeEntrega::getCodigo);
        this.cupons = indexar(cupons, Cupom::getCodigo);
        this.niveis = indexar(niveis, NivelClube::getCodigo);
        this.formasPagamento = indexar(formasPagamento, FormaPagamento::getCodigo);
    }

    public ResumoResponse calcular(CheckoutRequest request) {
        validarItens(request.itens());

        NivelClube nivel = buscarOuErro(niveis, request.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = validarRegiao(request.regiao());
        ModalidadeEntrega modalidade = buscarOuErro(modalidades, request.modalidadeEntrega(), "MODALIDADE_INVALIDA");

        BigDecimal pesoTotal = calcularPesoTotal(request.itens());
        if (!modalidade.isDisponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotalProdutos = calcularSubtotal(request.itens());

        Cupom cupom = null;
        if (request.cupom() != null && !request.cupom().isBlank()) {
            cupom = buscarOuErro(cupons, request.cupom(), "CUPOM_INVALIDO");
            if (!cupom.isAplicavel(subtotalProdutos, request.itens())) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
        }

        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        FormaPagamento forma = buscarOuErro(formasPagamento, request.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");

        if (!forma.isParcelamentoValido(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal frete = modalidade.calcularFrete(pesoTotal);
        if (nivel.isFreteGratis()) {
            frete = BigDecimal.ZERO.setScale(2);
        }

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (cupom != null) {
            descontoCupom = cupom.calcularDesconto(subtotalProdutos, frete, request.itens());
        }

        BigDecimal seguro = arredondar(subtotalProdutos.multiply(regiao.getTaxaSeguro()));

        BigDecimal totalPedido = subtotalProdutos
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro);

        if (!forma.isDisponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento pagamento = forma.calcular(totalPedido, parcelas);
        BigDecimal ajustePagamento = pagamento.totalFinal().subtract(totalPedido);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.getPrazoDias(),
                seguro,
                ajustePagamento,
                pagamento.totalFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela(),
                nivel.calcularCredito(subtotalProdutos),
                nivel.temBrinde(subtotalProdutos)
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

    private Regiao validarRegiao(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        Regiao regiao = Regiao.porCodigo(codigo);
        if (regiao == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        return regiao;
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(
                    item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return arredondar(subtotal);
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }

    private static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    private static <T> T buscarOuErro(Map<String, T> mapa, String codigo, String codigoErro) {
        if (codigo == null || codigo.isBlank()) {
            throw new CheckoutException(codigoErro);
        }
        T resultado = mapa.get(codigo);
        if (resultado == null) {
            throw new CheckoutException(codigoErro);
        }
        return resultado;
    }

    private static <T> Map<String, T> indexar(List<T> itens, Function<T, String> chave) {
        return itens.stream().collect(Collectors.toMap(chave, Function.identity()));
    }
}
