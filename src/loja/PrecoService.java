package loja;

public class PrecoService {
    public static final int FRETE_CENTAVOS = 1890;
    public static final int LIMITE_FRETE_GRATIS = 30000;

    public int desconto(int subtotalCentavos, String cupom) {
        if (cupom != null && cupom.trim().equalsIgnoreCase("BEMVINDO10")) {
            return subtotalCentavos / 10;
        }
        return 0;
    }

    public int frete(int subtotalCentavos) {
        return subtotalCentavos >= LIMITE_FRETE_GRATIS ? 0 : FRETE_CENTAVOS;
    }
}
