package com.loja.checkout.servico;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.dominio.Carrinho;
import com.loja.checkout.dominio.Cobranca;
import com.loja.checkout.dominio.Cupom;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.ErroPedido;
import com.loja.checkout.dominio.FormaPagamento;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.ModalidadeEntrega;
import com.loja.checkout.dominio.NivelClube;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.dominio.Resumo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class CalculadoraResumo {

    public Resumo calcular(ResumoRequest pedido) {
        Carrinho carrinho = lerCarrinho(pedido.itens());
        NivelClube nivel = ler(NivelClube.class, pedido.nivelClube(), CodigoErro.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = ler(Regiao.class, pedido.regiao(), CodigoErro.REGIAO_INVALIDA);
        ModalidadeEntrega modalidade = ler(ModalidadeEntrega.class, pedido.modalidadeEntrega(), CodigoErro.MODALIDADE_INVALIDA);
        if (!modalidade.atende(carrinho.pesoKg())) {
            throw new ErroPedido(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
        Cupom cupom = lerCupom(pedido.cupom());
        if (cupom != null) {
            cupom.verificarAplicavel(carrinho);
        }
        FormaPagamento forma = ler(FormaPagamento.class, pedido.formaPagamento(), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        if (parcelas < 1 || parcelas > forma.maxParcelas()) {
            throw new ErroPedido(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        BigDecimal subtotal = carrinho.subtotal();
        BigDecimal frete = Dinheiro.arredondar(nivel.aplicarFrete(modalidade.valorFrete(carrinho.pesoKg())));
        BigDecimal desconto = cupom == null ? Dinheiro.ZERO : Dinheiro.arredondar(cupom.desconto(carrinho, frete));
        BigDecimal seguro = regiao.seguroSobre(subtotal);
        BigDecimal total = subtotal.subtract(desconto).add(frete).add(seguro);

        forma.verificarDisponibilidade(total);
        Cobranca cobranca = forma.cobrar(total, parcelas);

        return new Resumo(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                cobranca.totalFinal().subtract(total),
                cobranca.totalFinal(),
                cobranca.parcelas(),
                cobranca.valorParcela(),
                nivel.creditoSobre(subtotal),
                nivel.temBrinde(subtotal));
    }

    private static Carrinho lerCarrinho(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new ErroPedido(CodigoErro.PEDIDO_INVALIDO);
        }
        List<Item> lista = new ArrayList<>();
        for (ItemRequest item : itens) {
            if (item == null
                    || item.precoUnitario() == null || item.precoUnitario().signum() <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().signum() <= 0) {
                throw new ErroPedido(CodigoErro.PEDIDO_INVALIDO);
            }
            lista.add(new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return new Carrinho(lista);
    }

    private static Cupom lerCupom(String nome) {
        if (nome == null || nome.isBlank()) {
            return null;
        }
        return ler(Cupom.class, nome, CodigoErro.CUPOM_INVALIDO);
    }

    private static <E extends Enum<E>> E ler(Class<E> tipo, String nome, CodigoErro erro) {
        if (nome == null) {
            throw new ErroPedido(erro);
        }
        try {
            return Enum.valueOf(tipo, nome);
        } catch (IllegalArgumentException e) {
            throw new ErroPedido(erro);
        }
    }
}
