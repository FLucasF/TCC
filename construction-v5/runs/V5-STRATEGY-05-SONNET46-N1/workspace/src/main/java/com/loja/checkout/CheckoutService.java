package com.loja.checkout;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.model.ItemCarrinho;
import com.loja.checkout.model.PedidoRequest;
import com.loja.checkout.model.PedidoResponse;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.regiao.Regiao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.loja.checkout.Arredondamento.ZERO;
import static com.loja.checkout.Arredondamento.arredondar;

@Service
public class CheckoutService {

    private final Map<String, ModalidadeEntrega> entregaMap;
    private final Map<String, Cupom> cupomMap;
    private final Map<String, NivelClube> clubeMap;
    private final Map<String, FormaPagamento> pagamentoMap;

    public CheckoutService(List<ModalidadeEntrega> entregas,
                           List<Cupom> cupons,
                           List<NivelClube> clubes,
                           List<FormaPagamento> pagamentos) {
        this.entregaMap = toMap(entregas, ModalidadeEntrega::codigo);
        this.cupomMap = toMap(cupons, Cupom::codigo);
        this.clubeMap = toMap(clubes, NivelClube::codigo);
        this.pagamentoMap = toMap(pagamentos, FormaPagamento::codigo);
    }

    private static <T> Map<String, T> toMap(List<T> list, Function<T, String> key) {
        return list.stream().collect(Collectors.toMap(key, Function.identity()));
    }

    public PedidoResponse calcular(PedidoRequest req) {
        validarItens(req.itens());

        NivelClube clube = buscar(req.nivelClube(), clubeMap, "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = buscarRegiao(req.regiao());
        ModalidadeEntrega entrega = buscar(req.modalidadeEntrega(), entregaMap, "MODALIDADE_INVALIDA");

        BigDecimal pesoTotal = calcularPesoTotal(req.itens());
        entrega.validarDisponibilidade(pesoTotal);

        BigDecimal subtotal = arredondar(calcularSubtotalBruto(req.itens()));

        Cupom cupom = req.cupom() != null
                ? buscar(req.cupom(), cupomMap, "CUPOM_INVALIDO")
                : null;
        if (cupom != null) cupom.validarAplicabilidade(subtotal);

        FormaPagamento pagamento = buscar(req.formaPagamento(), pagamentoMap, "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = req.parcelas() != null ? req.parcelas() : 1;
        pagamento.validarParcelas(parcelas);

        BigDecimal freteNatural = arredondar(entrega.calcularFrete(pesoTotal));
        BigDecimal frete = clube.temFreteGratis() ? ZERO : freteNatural;

        BigDecimal descontoCupom = cupom != null
                ? arredondar(cupom.calcularDesconto(subtotal, req.itens(), frete))
                : ZERO;

        BigDecimal seguro = arredondar(subtotal.multiply(regiao.taxaSeguro()));

        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        pagamento.validarDisponibilidade(totalPedido);

        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);

        BigDecimal credito = clube.calcularCredito(subtotal);
        boolean brinde = clube.temBrinde(subtotal);

        return new PedidoResponse(
                subtotal,
                descontoCupom,
                frete,
                entrega.prazoEntregaDias(),
                seguro,
                resultado.ajuste(),
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                credito,
                brinde);
    }

    private void validarItens(List<ItemCarrinho> itens) {
        if (itens == null || itens.isEmpty()) throw new CheckoutException("PEDIDO_INVALIDO");
        for (ItemCarrinho item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private BigDecimal calcularSubtotalBruto(List<ItemCarrinho> itens) {
        return itens.stream()
                .map(i -> i.precoUnitario().multiply(BigDecimal.valueOf(i.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularPesoTotal(List<ItemCarrinho> itens) {
        return itens.stream()
                .map(i -> i.pesoKg().multiply(BigDecimal.valueOf(i.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Regiao buscarRegiao(String codigo) {
        if (codigo == null) throw new CheckoutException("REGIAO_INVALIDA");
        try {
            return Regiao.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private <T> T buscar(String codigo, Map<String, T> mapa, String erroCodigo) {
        if (codigo == null || !mapa.containsKey(codigo)) {
            throw new CheckoutException(erroCodigo);
        }
        return mapa.get(codigo);
    }
}
