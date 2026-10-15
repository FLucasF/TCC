package com.loja.resumo.calculo;

import com.loja.resumo.api.ItemRequest;
import com.loja.resumo.api.ResumoRequest;
import com.loja.resumo.api.ResumoResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.loja.resumo.calculo.Dinheiro.ZERO;
import static com.loja.resumo.calculo.Dinheiro.arredonda;

@Service
public class ResumoCompraService {

    public ResumoResponse calcular(ResumoRequest req) {
        Pedido pedido = montarPedido(req.itens());
        NivelClube nivel = buscar(NivelClube.class, req.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = buscar(Regiao.class, req.regiao(), "REGIAO_INVALIDA");
        Entrega entrega = buscar(Entrega.class, req.modalidadeEntrega(), "MODALIDADE_INVALIDA");
        if (!entrega.atende(pedido.pesoKg())) {
            throw new ResumoException("MODALIDADE_INDISPONIVEL");
        }
        Optional<Cupom> cupom = cupomValido(req.cupom(), pedido);
        FormaPagamento forma = buscar(FormaPagamento.class, req.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!forma.parcelamentoPermitido(parcelas)) {
            throw new ResumoException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal subtotal = pedido.subtotal();
        BigDecimal frete = nivel.aplicarFrete(arredonda(entrega.frete(pedido.pesoKg())));
        BigDecimal desconto = cupom.map(c -> arredonda(c.desconto(pedido, frete))).orElse(ZERO);
        BigDecimal seguro = regiao.seguroSobre(subtotal);
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);
        if (!forma.disponivel(totalPedido)) {
            throw new ResumoException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        Pagamento pagamento = forma.calcular(totalPedido, parcelas);
        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                entrega.prazoDias(),
                seguro,
                pagamento.totalFinal().subtract(totalPedido),
                pagamento.totalFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela(),
                nivel.creditoSobre(subtotal),
                nivel.temBrinde(subtotal));
    }

    private Pedido montarPedido(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty() || itens.stream().anyMatch(Objects::isNull)) {
            throw new ResumoException("PEDIDO_INVALIDO");
        }
        return new Pedido(itens.stream()
                .map(i -> new Item(i.precoUnitario(), i.quantidade(), i.pesoKg()))
                .toList());
    }

    private Optional<Cupom> cupomValido(String codigo, Pedido pedido) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        Cupom cupom = buscar(Cupom.class, codigo, "CUPOM_INVALIDO");
        if (!cupom.aplicavel(pedido)) {
            throw new ResumoException("CUPOM_NAO_APLICAVEL");
        }
        return Optional.of(cupom);
    }

    private static <E extends Enum<E>> E buscar(Class<E> tipo, String codigo, String erro) {
        return Arrays.stream(tipo.getEnumConstants())
                .filter(e -> e.name().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new ResumoException(erro));
    }
}
