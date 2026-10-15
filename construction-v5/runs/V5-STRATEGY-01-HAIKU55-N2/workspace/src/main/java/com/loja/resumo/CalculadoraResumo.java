package com.loja.resumo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
class CalculadoraResumo {

    ResumoCompra calcular(ResumoRequest pedido) {
        List<ItemCompra> itens = itens(pedido.itens());
        NivelClube nivel = NivelClube.de(pedido.nivelClube());
        Regiao regiao = Regiao.de(pedido.regiao());
        ModalidadeEntrega modalidade = ModalidadeEntrega.de(pedido.modalidadeEntrega());
        Compra compra = new Compra(itens);
        modalidade.validarDisponibilidade(compra.pesoKg());

        Optional<Cupom> cupom = Optional.ofNullable(pedido.cupom()).map(Cupom::de);
        cupom.ifPresent(c -> c.validar(compra));

        FormaPagamento forma = FormaPagamento.de(pedido.formaPagamento());
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        forma.validarParcelas(parcelas);

        BigDecimal subtotal = compra.subtotal();
        BigDecimal frete = Dinheiro.arredondar(nivel.ajustarFrete(modalidade.frete(compra.pesoKg())));
        BigDecimal desconto = cupom
                .map(c -> Dinheiro.arredondar(c.desconto(compra, frete)))
                .orElse(Dinheiro.ZERO);
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);

        forma.validarDisponibilidade(totalPedido);
        ResultadoPagamento pagamento = forma.calcular(totalPedido, parcelas);

        return new ResumoCompra(
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

    private static List<ItemCompra> itens(List<ResumoRequest.Item> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new ErroCompra(CodigoErro.PEDIDO_INVALIDO);
        }
        return itens.stream().map(CalculadoraResumo::item).toList();
    }

    private static ItemCompra item(ResumoRequest.Item item) {
        if (item == null
                || item.precoUnitario() == null || item.precoUnitario().signum() <= 0
                || item.quantidade() == null || item.quantidade() <= 0
                || item.pesoKg() == null || item.pesoKg().signum() <= 0) {
            throw new ErroCompra(CodigoErro.PEDIDO_INVALIDO);
        }
        return new ItemCompra(item.precoUnitario(), item.quantidade(), item.pesoKg());
    }
}
