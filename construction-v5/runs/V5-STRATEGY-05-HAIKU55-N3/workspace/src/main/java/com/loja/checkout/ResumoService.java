package com.loja.checkout;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ResumoService {

    public ResumoResponse calcular(ResumoRequest req) {
        List<ItemPedido> itens = validarItens(req.itens());
        BigDecimal subtotal = itens.stream().map(item -> item.subtotal()).reduce(Dinheiro.ZERO, BigDecimal::add);
        BigDecimal peso = itens.stream().map(item -> item.peso()).reduce(BigDecimal.ZERO, BigDecimal::add);

        NivelClube nivel = buscar(NivelClube.values(), req.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = buscar(Regiao.values(), req.regiao(), "REGIAO_INVALIDA");
        ModalidadeEntrega modalidade = buscar(ModalidadeEntrega.values(), req.modalidadeEntrega(), "MODALIDADE_INVALIDA");
        if (!modalidade.disponivel(peso)) {
            throw new ResumoException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal frete = nivel.temFreteGratis() ? Dinheiro.ZERO : modalidade.frete(peso);
        Pedido pedido = new Pedido(itens, subtotal, frete);
        BigDecimal desconto = Optional.ofNullable(req.cupom())
                .filter(codigo -> !codigo.isBlank())
                .map(codigo -> descontoDoCupom(codigo, pedido))
                .orElse(Dinheiro.ZERO);

        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);

        FormaPagamento forma = buscar(FormaPagamento.values(), req.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = Optional.ofNullable(req.parcelas()).orElse(1);
        if (!forma.parcelasPermitidas(parcelas)) {
            throw new ResumoException("PARCELAMENTO_INVALIDO");
        }
        if (!forma.disponivel(totalPedido)) {
            throw new ResumoException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        Pagamento pagamento = forma.calcular(totalPedido, parcelas);

        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                pagamento.totalFinal().subtract(totalPedido),
                pagamento.totalFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela(),
                nivel.credito(subtotal),
                nivel.temBrinde(subtotal));
    }

    private static List<ItemPedido> validarItens(List<ResumoRequest.Item> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new ResumoException("PEDIDO_INVALIDO");
        }
        return itens.stream().map(ResumoService::validarItem).toList();
    }

    private static ItemPedido validarItem(ResumoRequest.Item item) {
        if (item == null
                || !positivo(item.precoUnitario())
                || item.quantidade() == null || item.quantidade() <= 0
                || !positivo(item.pesoKg())) {
            throw new ResumoException("PEDIDO_INVALIDO");
        }
        return new ItemPedido(item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    private static BigDecimal descontoDoCupom(String codigo, Pedido pedido) {
        Cupom cupom = buscar(Cupom.values(), codigo, "CUPOM_INVALIDO");
        if (!cupom.aplicavel(pedido)) {
            throw new ResumoException("CUPOM_NAO_APLICAVEL");
        }
        return cupom.desconto(pedido);
    }

    private static <E extends Enum<E>> E buscar(E[] valores, String codigo, String erro) {
        return Arrays.stream(valores)
                .filter(valor -> valor.name().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new ResumoException(erro));
    }
}
