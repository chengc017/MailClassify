
import java.util.Iterator;
import java.util.StringTokenizer;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Leandro Carlos Rodrigues <leandro.carlos.rodrigues@gmail.com>
 */
public class TestDNSBL {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
//        String dnsblDomain = "bl.spamcop.net";
//        String ipAddress = "2.39.80.25";
        String dnsblDomain = "list.dnswl.org";
        String ipAddress = "50.115.210.248";
        StringTokenizer tokenizer = new StringTokenizer(ipAddress, ".");
        ipAddress = tokenizer.nextToken();
        ipAddress = tokenizer.nextToken() + "." + ipAddress;
        ipAddress = tokenizer.nextToken() + "." + ipAddress;
        ipAddress = tokenizer.nextToken() + "." + ipAddress;
        String query = ipAddress + "." + dnsblDomain;

        Lookup lookup = new Lookup(query, Type.ANY);
        Resolver resolver = new SimpleResolver();
        lookup.setResolver(resolver);
        lookup.setCache(null);
        Record[] records = lookup.run();
        if (lookup.getResult() == Lookup.SUCCESSFUL) {
            String responseMessage = "";
            String listingType = "";
            for (int i = 0; i < records.length; i++) {
                if (records[i] instanceof TXTRecord) {
                    TXTRecord txt = (TXTRecord) records[i];
                    for (Iterator j = txt.getStrings().iterator(); j.hasNext();) {
                        responseMessage += (String) j.next();
                    }
                } else if (records[i] instanceof ARecord) {
                    listingType = ((ARecord) records[i]).getAddress().getHostAddress();
                }
            }

            System.err.println("Found!");
            System.err.println("Response Message: " + responseMessage);
            System.err.println("Listing Type: " + listingType);
        } else if (lookup.getResult() == Lookup.HOST_NOT_FOUND) {
            System.err.println("Not found.");
        } else {
            System.err.println("Error!");
        }
    }
}
