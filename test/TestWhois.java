

import org.apache.commons.net.whois.WhoisClient;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Leandro Carlos Rodrigues <leandro.carlos.rodrigues@gmail.com>
 */
public class TestWhois {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        String domainName = "allchemistry.com.br";
        WhoisClient whois = new WhoisClient();
        whois.connect(WhoisClient.DEFAULT_HOST);
        String result = whois.query(domainName);
        whois.disconnect();
        System.out.println(result);
    }
}
