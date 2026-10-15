package com.loja.checkout.calculo;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dominio.AjustePagamento;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ServicoResumo {

    public ResumoResponse calcular(ResumoRequest req) {
        List<Item> itens = validarItens(req.itens());

        NivelClube nivel = NivelClube.de(req.nivelClube())
                .orElseThrow(() -> new ErroPedido("NIVEL_CLUBE_INVALIDO"));

        Regiao regiao = Regiao.de(req.regiao())
                .orElseThrow(() -> new ErroPedido("REGIAO_INVALIDA"));

        ModalidadeEntrega modalidade = ModalidadeEntrega.de(req.modalidadeEntrega())
                .orElseThrow(() -> new ErroPedido("MODALIDADE_INVALIDA"));

        BigDecimal pesoTotal = itens.stream()
                .map(Item::pesoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!modalidade.aceita(pesoTotal)) {
            throw new ErroPedido("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotal = Dinheiro.arredondar(
                itens.stream().map(Item::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add));

        BigDecimal freteBruto = Dinheiro.arredondar(modalidade.custo(pesoTotal));
        BigDecimal frete = nivel.pagaFrete() ? freteBruto : Dinheiro.ZERO;

        Cupom cupom = resolverCupom(req.cupom(), itens, subtotal);
        BigDecimal descontoCupom = cupom == null
                ? Dinheiro.ZERO
                : Dinheiro.arredondar(cupom.desconto(itens, subtotal, frete));

        FormaPagamento forma = FormaPagamento.de(req.formaPagamento())
                .orElseThrow(() -> new ErroPedido("FORMA_PAGAMENTO_INVALIDA"));

        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!forma.parcelasValidas(parcelas)) {
            throw new ErroPedido("PARCELAMENTO_INVALIDO");
        }

        BigDecimal seguro = Dinheiro.arredondar(subtotal.multiply(regiao.percentualSeguro()));
        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        if (!forma.aceita(totalPedido)) {
            throw new ErroPedido("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        AjustePagamento aj = forma.calcular(totalPedido, parcelas);
        BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.add(aj.ajuste()));
        BigDecimal credito = Dinheiro.arredondar(nivel.credito(subtotal));

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                Dinheiro.arredondar(aj.ajuste()),
                totalFinal,
                parcelas,
                Dinheiro.arredondar(aj.valorParcela()),
                credito,
                nivel.temBrinde(subtotal)
        );
    }

    private List<Item> validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) throw new ErroPedido("PEDIDO_INVALIDO");
        List<Item> resultado = new ArrayList<>(itens.size());
        for (ItemRequest it : itens) {
            if (it == null
                    || it.precoUnitario() == null
                    || it.quantidade() == null
                    || it.pesoKg() == null
                    || it.precoUnitario().signum() <= 0
                    || it.quantidade() <= 0
                    || it.pesoKg().signum() <= 0) {
                throw new ErroPedido("PEDIDO_INVALIDO");
            }
            resultado.add(new Item(it.nome(), it.precoUnitario(), it.quantidade(), it.pesoKg()));
        }
        return resultado;
    }

    private Cupom resolverCupom(String codigo, List<Item> itens, BigDecimal subtotal) {
        if (codigo == null) return null;
        Cupom cupom = Cupom.de(codigo).orElseThrow(() -> new ErroPedido("CUPOM_INVALIDO"));
        if (!cupom.aplicavel(itens, subtotal)) throw new ErroPedido("CUPOM_NAO_APLICAVEL");
        return cupom;
    }
}
