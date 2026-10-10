package com.loja.checkout.service;

import com.loja.checkout.domain.Moeda;
import com.loja.checkout.domain.Regiao;
import com.loja.checkout.domain.clube.NivelClube;
import com.loja.checkout.domain.cupom.Cupom;
import com.loja.checkout.domain.entrega.ModalidadeEntrega;
import com.loja.checkout.domain.pagamento.FormaPagamento;
import com.loja.checkout.domain.pagamento.ResultadoPagamento;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
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
    private final Map<String, NivelClube> niveisClube;
    private final Map<String, FormaPagamento> formasPagamento;

    public CheckoutService(List<ModalidadeEntrega> modalidades,
                           List<Cupom> cupons,
                           List<NivelClube> niveisClube,
                           List<FormaPagamento> formasPagamento) {
        this.modalidades = indexar(modalidades, ModalidadeEntrega::codigo);
        this.cupons = indexar(cupons, Cupom::codigo);
        this.niveisClube = indexar(niveisClube, NivelClube::codigo);
        this.formasPagamento = indexar(formasPagamento, FormaPagamento::codigo);
    }

    public ResumoResponse calcular(ResumoRequest req) {
        validarItens(req.itens());

        BigDecimal subtotal = calcularSubtotal(req.itens());
        BigDecimal pesoTotal = calcularPeso(req.itens());

        NivelClube nivel = buscar(niveisClube, req.nivelClube(), "NIVEL_CLUBE_INVALIDO");

        Regiao regiao = Regiao.buscar(req.regiao())
                .orElseThrow(() -> new CheckoutException("REGIAO_INVALIDA"));

        ModalidadeEntrega modalidade = buscar(modalidades, req.modalidadeEntrega(), "MODALIDADE_INVALIDA");

        if (!modalidade.disponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal frete = nivel.freteGratis()
                ? BigDecimal.ZERO.setScale(2)
                : modalidade.calcularFrete(pesoTotal);

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (req.cupom() != null && !req.cupom().isEmpty()) {
            Cupom cupom = buscar(cupons, req.cupom(), "CUPOM_INVALIDO");
            if (!cupom.aplicavel(req.itens(), subtotal, frete)) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = cupom.calcularDesconto(req.itens(), subtotal, frete);
        }

        int parcelas = req.parcelas() != null ? req.parcelas() : 1;
        FormaPagamento pagamento = buscar(formasPagamento, req.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");

        if (!pagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal seguro = regiao.calcularSeguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        if (!pagamento.disponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);
        BigDecimal ajuste = resultado.totalFinal().subtract(totalPedido);

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajuste,
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                nivel.calcularCredito(subtotal),
                nivel.brinde(subtotal));
    }

    private void validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemRequest item : itens) {
            if (item == null
                    || item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
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
        return Moeda.arredondar(subtotal);
    }

    private BigDecimal calcularPeso(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }

    private static <T> T buscar(Map<String, T> mapa, String codigo, String codigoErro) {
        if (codigo == null) throw new CheckoutException(codigoErro);
        T valor = mapa.get(codigo);
        if (valor == null) throw new CheckoutException(codigoErro);
        return valor;
    }

    private static <T> Map<String, T> indexar(List<T> lista, Function<T, String> chave) {
        return lista.stream().collect(Collectors.toMap(chave, Function.identity()));
    }
}
