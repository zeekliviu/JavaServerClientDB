import java.io.Serializable;

public class Response implements Serializable {
    private String numeCarte;
    private byte[] pdf;

    public Response(String numeCarte, byte[] pdf) {
        this.numeCarte = numeCarte;
        this.pdf = pdf;
    }

    public String getNumeCarte() {
        return numeCarte;
    }

    public byte[] getContinut() {
        return pdf;
    }
}
