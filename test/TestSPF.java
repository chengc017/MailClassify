
import org.apache.james.jspf.executor.SPFResult;
import org.apache.james.jspf.impl.DefaultSPF;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Leandro Carlos Rodrigues <leandro.carlos.rodrigues@gmail.com>
 */
public class TestSPF {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        DefaultSPF spf = new DefaultSPF();
        SPFResult res = spf.checkSPF("ff02::2", "temp@srvnetjogos.info", "srvnetjogos.info");
        System.out.println(res.getExplanation());
        System.out.println(res.getHeader());
        System.out.println(res.getHeaderName());
        System.out.println(res.getHeaderText());
        System.out.println(res.getResult());
    }
}
