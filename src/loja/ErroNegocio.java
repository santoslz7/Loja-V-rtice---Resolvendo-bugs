package loja;

public class ErroNegocio extends RuntimeException {
    private final int codigoHttp;

    public ErroNegocio(int codigoHttp, String mensagem) {
        super(mensagem);
        this.codigoHttp = codigoHttp;
    }

    public int getCodigoHttp() { return codigoHttp; }
}
