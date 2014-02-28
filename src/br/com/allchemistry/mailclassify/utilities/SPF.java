/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.allchemistry.mailclassify.utilities;

import java.util.HashMap;
import org.apache.james.jspf.executor.SPFResult;
import org.apache.james.jspf.impl.DefaultSPF;

/**
 *
 * @author Leandro Carlos Rodrigues <leandro.carlos.rodrigues@gmail.com>
 */
public class SPF {
    
    private static final DefaultSPF SPF = new DefaultSPF();
    
    private static final HashMap<String,SPFResult> resultMap =
            new HashMap<String,SPFResult>();
    
    public static SPFResult checkSPF(String ipAddress,
            String mailFrom, String hostName) {
        String key = ipAddress + ";" + mailFrom + ";" + hostName;
        if (resultMap.containsKey(key)) {
            return resultMap.get(key);
        } else {
            SPFResult res = SPF.checkSPF(ipAddress, mailFrom, hostName);
            resultMap.put(key, res);
            return res;
        }
    }
}
