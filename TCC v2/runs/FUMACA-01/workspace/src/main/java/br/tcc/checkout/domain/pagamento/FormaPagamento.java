package br.tcc.checkout.domain.pagamento;

import br.tcc.checkout.domain.Arredondador;

public enum FormaPagamento {
    PIX {
        @Override
        public double calcularAjuste(double total, int parcelas) {
            double desconto = Arredondador.arredondar(total * 0.05);
            return -desconto;
        }

        @Override
        public boolean eParcelamentoValido(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean eDisponivelPara(double total) {
            return true;
        }
    },
    CARTAO {
        @Override
        public double calcularAjuste(double total, int parcelas) {
            if (parcelas <= 3) {
                return 0.0;
            }
            double taxaMensal = 0.0199;
            double parcela = total * taxaMensal / (1.0 - Math.pow(1.0 + taxaMensal, -parcelas));
            parcela = Arredondador.arredondar(parcela);
            double totalComJuros = parcela * parcelas;
            return Arredondador.arredondar(totalComJuros - total);
        }

        @Override
        public boolean eParcelamentoValido(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public boolean eDisponivelPara(double total) {
            return true;
        }
    },
    BOLETO {
        @Override
        public double calcularAjuste(double total, int parcelas) {
            return 3.49;
        }

        @Override
        public boolean eParcelamentoValido(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean eDisponivelPara(double total) {
            return total <= 1000.00;
        }
    };

    public abstract double calcularAjuste(double total, int parcelas);

    public abstract boolean eParcelamentoValido(int parcelas);

    public abstract boolean eDisponivelPara(double total);

    public static FormaPagamento porNome(String nome) {
        if (nome == null) return null;
        try {
            return FormaPagamento.valueOf(nome.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
