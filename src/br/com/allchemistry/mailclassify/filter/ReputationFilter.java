/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.allchemistry.mailclassify.filter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Matcher;
import javax.mail.Message;
import javax.mail.MessagingException;
import br.com.allchemistry.mailclassify.ExpirableMap;
import br.com.allchemistry.mailclassify.Main;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

/**
 *
 * @author Leandro Carlos Rodrigues <leandro.carlos.rodrigues@gmail.com>
 */
public final class ReputationFilter extends ClassifyFilter {
    
    private short TIMEOUT;
    private float PRECISION;
    private final ArrayList<Server> SERVER_LIST;
    private final ExpirableMap<String,Boolean> resultMap;
    
    public ReputationFilter(String descriptor,
            int timeout) throws Exception {
        super(descriptor);
        SERVER_LIST = new ArrayList<Server>();
        PRECISION = 0.5f;
        setTimeout(timeout);
        resultMap = new ExpirableMap<String,Boolean>(7);
    }
    
    public ReputationFilter(String descriptor,
            int timeout, float precision) throws Exception {
        super(descriptor);
        SERVER_LIST = new ArrayList<Server>();
        setPrecision(precision);
        setTimeout(timeout);
        resultMap = new ExpirableMap<String,Boolean>(7);
    }
    
    private ReputationFilter(ObjectInputStream ois)
            throws IOException, Exception {
        super(ois);
        TIMEOUT = ois.readShort();
        PRECISION = ois.readFloat();
        int size = ois.readInt();
        SERVER_LIST = new ArrayList<Server>(size);
        for (int i = 0; i < size; i++) {
            Server server = new Server(ois);
            SERVER_LIST.add(i, server);
        }
        resultMap = new ExpirableMap<String,Boolean>(ois);
    }
    
    protected void setPrecision(float precision) throws Exception {
        if (precision <= 0.5f || precision >= 1.0f) {
            throw new Exception("Invalid value.");
        } else {
            PRECISION = precision;
        }
    }
    
    protected void clearPrecision() {
        PRECISION = 0.5f;
    }

    protected void setTimeout(int timeout) throws Exception {
        if (timeout <= 0 || timeout > Short.MAX_VALUE) {
            throw new Exception("Invalid value.");
        } else {
            TIMEOUT = (short) timeout;
        }
    }
    
    @Override
    public void store() throws IOException {
        String fileName = "." + getDescriptor() + ".filter";
        FileOutputStream fos = new FileOutputStream(fileName);
        try {
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeUTF("ReputationFilter");
            oos.writeInt(1); // version
            super.store(oos);
            oos.writeShort(TIMEOUT);
            oos.writeFloat(PRECISION);
            int size = SERVER_LIST.size();
            oos.writeInt(size);
            for (int i = 0; i < size; i++) {
                SERVER_LIST.get(i).store(oos);
            }
            resultMap.store(oos);
            oos.flush();
        } finally {
            fos.close();
        }
        File fileNew = new File("." + getDescriptor() + ".filter");
        File fileOld = new File(getDescriptor() + ".filter");
        if (!fileOld.exists() || fileOld.delete()) {
            fileNew.renameTo(fileOld);
        }
    }
    
    @Override
    public void downsizeCache(float ratio) {
        try {
            resultMap.downsize(ratio);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public static ReputationFilter loadOrCreate(String descriptor,
            int timeout) throws Exception {
        ReputationFilter filter = load(descriptor);
        if (filter == null) {
            filter = new ReputationFilter(descriptor, timeout);
        } else {
            filter.setDescriptor(descriptor);
            filter.clearPrecision();
            filter.setTimeout(timeout);
//            filter.clearServers();
        }
        return filter;
    }
    
    public static ReputationFilter loadOrCreate(String descriptor,
            int timeout, float precision) throws Exception {
        ReputationFilter filter = load(descriptor);
        if (filter == null) {
            filter = new ReputationFilter(descriptor, timeout, precision);
        } else {
            filter.setDescriptor(descriptor);
            filter.setPrecision(precision);
            filter.setTimeout(timeout);
//            filter.clearServers();
        }
        return filter;
    }
    
    public static ReputationFilter load(String descriptor) {
        String fileName = descriptor + ".filter";
        File file = new File(fileName);
        if (file.exists()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                try {
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    String className = ois.readUTF();
                    if (className.equals("ReputationFilter")) {
                        int version = ois.readInt();
                        if (version == 1) {
                            return new ReputationFilter(ois);
                        } else {
                            throw new IOException("Version not implemented.");
                        }
                    } else {
                        throw new IOException("Incompatible file.");
                    }
                } catch (Exception exception) {
                    throw new IOException("Incompatible file.", exception);
                } finally {
                    fis.close();
                }
            } catch (Exception exception) {
                exception.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }
    
    public void addServer(String host, boolean output) {
        Server server = new Server(host, output);
        if (!SERVER_LIST.contains(server)) {
            SERVER_LIST.add(server);
        }
    }
    
    public void setServers(Map<String,Boolean> serverMap) {
        int n = serverMap.size();
        ArrayList<Server> serverList = new ArrayList<Server>(n);
        for (String host : serverMap.keySet()) {
            boolean found = false;
            for (Server server : SERVER_LIST) {
                if (server.getDescriptor().equals(host)) {
                    serverList.add(server);
                    found = true;
                }
            }
            if (!found) {
                boolean output = serverMap.get(host);
                Server server = new Server(host, output);
                serverList.add(server);
            }
        }
        SERVER_LIST.clear();
        SERVER_LIST.addAll(serverList);
    }
    
    private static String getHostIP(
            Message message) throws MessagingException {
        String[] headers = message.getHeader("Received");
        if (headers == null) {
            return null;
        } else {
            for (String header : headers) {
                if (header.startsWith("from ") && header.contains("by ")) {
                    int beginIndex = 5;
                    int endIndex = header.indexOf("by ", beginIndex);
                    String from = header.substring(beginIndex, endIndex).trim();
                    Matcher matcher = IpFilter.PATTERN_ADDRESS.matcher(from);
                    while (matcher.find()) {
                        String ip = matcher.group(2);
                        return ip;
                    }
                } else {
                    return null;
                }
            }
            return null;
        }
    }
    
    @Override
    public final byte trainingNegative(Message message) {
        long startTime = System.currentTimeMillis();
        byte output = OUTPUT_INCONCLUSIVE;
        try {
            String ip = getHostIP(message);
            if (ip == null) {
                output = OUTPUT_INCONCLUSIVE;
            } else {
                for (Server server : SERVER_LIST) {
                    if (System.currentTimeMillis() - startTime > TIMEOUT) {
                        break;
                    } else if (server.getHitRatio() > Math.random()) {
                        if (server.isListed(ip)) {
                            if (server.isOutputPositive()) {
                                server.addPositiveOutput();
                                server.addErrorSuccess();
                                output = OUTPUT_POSITIVE;
                            } else {
                                server.addNegativeOutput();
                                server.addErrorFailure();
                                output = OUTPUT_NEGATIVE;
                            }
                            {
                                String after = SERVER_LIST.toString();
                                Collections.sort(SERVER_LIST);
                                String before = SERVER_LIST.toString();
                                if (!after.equals(before)) {
                                    System.out.println(SERVER_LIST);
                                }
                            }
                            if (server.getHitRatio() > PRECISION) {
                                break;
                            }
                        } else {
                            server.addInconclusiveOutput();
                        }
                    }
                }
            }
        } catch (Exception exception) {
            output = OUTPUT_INCONCLUSIVE;
        } finally {
            if (output == OUTPUT_NEGATIVE) {
                addSuccessNegative();
                addNegativeOutput();
            } else if (output == OUTPUT_POSITIVE) {
                addFailurePositive();
                addPositiveOutput();
            } else {
                addInconclusiveOutput();
            }
            long endTime = System.currentTimeMillis();
            addTimeSpent(endTime - startTime);
            return output;
        }
    }
    
    @Override
    public final byte trainingPositive(Message message) {
        long startTime = System.currentTimeMillis();
        byte output = OUTPUT_INCONCLUSIVE;
        try {
            String ip = getHostIP(message);
            if (ip == null) {
                output = OUTPUT_INCONCLUSIVE;
            } else {
                for (Server server : SERVER_LIST) {
                    if (System.currentTimeMillis() - startTime > TIMEOUT) {
                        break;
                    } else if (server.getHitRatio() > Math.random()) {
                        if (server.isListed(ip)) {
                            if (server.isOutputPositive()) {
                                server.addPositiveOutput();
                                server.addErrorFailure();
                                output = OUTPUT_POSITIVE;
                            } else {
                                server.addNegativeOutput();
                                server.addErrorSuccess();
                                output = OUTPUT_NEGATIVE;
                            }
                            {
                                String after = SERVER_LIST.toString();
                                Collections.sort(SERVER_LIST);
                                String before = SERVER_LIST.toString();
                                if (!after.equals(before)) {
                                    System.out.println(SERVER_LIST);
                                }
                            }
                            if (server.getHitRatio() > PRECISION) {
                                break;
                            }
                        } else {
                            server.addInconclusiveOutput();
                        }
                    }
                }
            }
        } catch (Exception exception) {
            output = OUTPUT_INCONCLUSIVE;
        } finally {
            if (output == OUTPUT_NEGATIVE) {
                addFailureNegative();
                addNegativeOutput();
            } else if (output == OUTPUT_POSITIVE) {
                addSuccessPositive();
                addPositiveOutput();
            } else {
                addInconclusiveOutput();
            }
            long endTime = System.currentTimeMillis();
            addTimeSpent(endTime - startTime);
            return output;
        }
    }

    @Override
    public final byte classify(Message message) {
        long startTime = System.currentTimeMillis();
        byte output = OUTPUT_INCONCLUSIVE;
        try {
            String ip = getHostIP(message);
            if (ip == null) {
                output = OUTPUT_INCONCLUSIVE;
            } else {
                for (Server server : SERVER_LIST) {
                    if (System.currentTimeMillis() - startTime > TIMEOUT) {
                        break;
                    } else if (server.getHitRatio() > PRECISION) {
                        if (server.isListed(ip)) {
                            if (server.isOutputPositive()) {
                                server.addPositiveOutput();
                                output = OUTPUT_POSITIVE;
                            } else {
                                server.addNegativeOutput();
                                output = OUTPUT_NEGATIVE;
                            }
                        } else {
                            server.addInconclusiveOutput();
                        }
                    }
                }
            }
        } catch (Exception exception) {
            output = OUTPUT_INCONCLUSIVE;
        } finally {
            long endTime = System.currentTimeMillis();
            addTimeSpent(endTime - startTime);
            return output;
        }
    }
    
    @Override
    public void printStatistics(PrintStream printStream) {
        super.printStatistics(printStream);
        for (Server server : SERVER_LIST) {
            printStream.println("Server: " + server.getDescriptor());
            printStream.println("Time spent: " + server.getTimeSpent());
            printStream.println("Conclusive ratio: " + server.getConclusiveRatio());
            printStream.println("Hit ratio: " + server.getHitRatio());
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        ReputationFilter mailFilter = ReputationFilter.loadOrCreate("reputation", 500);
        TreeMap<String,Boolean> serverMap = new TreeMap<String,Boolean>();
        serverMap.put("list.dnswl.org", false);
        serverMap.put("zen.spamhaus.org", true);
        serverMap.put("b.barracudacentral.org", true);
        serverMap.put("wl.nszones.com", false);
        mailFilter.setServers(serverMap);

//        mailFilter.addServer("bogons.cymru.com", true);
//        mailFilter.addServer("korea.services.net", true);
//        mailFilter.addServer("duinv.aupads.org", true);
//        mailFilter.addServer("short.rbl.jp", true);
//        mailFilter.addServer("residential.block.transip.nl", true);
//        mailFilter.addServer("images.rbl.msrbl.net", true);
//        mailFilter.addServer("query.bondedsender.org", false);
//        mailFilter.addServer("drone.abuse.ch", true);
//        mailFilter.addServer("http.dnsbl.sorbs.net", true);
//        mailFilter.addServer("relays.bl.gweep.ca", true);
//        mailFilter.addServer("pbl.spamhaus.org", true);
//        mailFilter.addServer("dul.ru", true);
//        mailFilter.addServer("owps.dnsbl.net.au", true);
//        mailFilter.addServer("phishing.rbl.msrbl.net", true);
//        mailFilter.addServer("spam.rbl.msrbl.net", true);
//        mailFilter.addServer("tor.ahbl.org", true);
//        mailFilter.addServer("osrs.dnsbl.net.au", true);
//        mailFilter.addServer("spam.dnsbl.sorbs.net", true);
//        mailFilter.addServer("rbl.megarbl.net", true);
//        mailFilter.addServer("tor.dnsbl.sectoor.de", true);
//        mailFilter.addServer("combined.abuse.ch", true);
//        mailFilter.addServer("t3direct.dnsbl.net.au", true);
//        mailFilter.addServer("ips.whitelisted.org", false);
//        mailFilter.addServer("sohul.habeas.com", false);
//        mailFilter.addServer("noptr.spamrats.com", true);
//        mailFilter.addServer("spamrbl.imp.ch", true);
//        mailFilter.addServer("cdl.anti-spam.org.cn", true);
//        mailFilter.addServer("osps.dnsbl.net.au", true);
//        mailFilter.addServer("probes.dnsbl.net.au", true);
//        mailFilter.addServer("accredit.habeas.com", false);
//        mailFilter.addServer("hul.habeas.com", false);
//        mailFilter.addServer("cabal.web-o-trust.org", false);
//        mailFilter.addServer("blackholes.five-ten-sg.com", true);
//        mailFilter.addServer("dnsbl-1.uceprotect.net", true);
//        mailFilter.addServer("rdts.dnsbl.net.au", true);
//        mailFilter.addServer("rmst.dnsbl.net.au", true);
//        mailFilter.addServer("dnsbl-2.uceprotect.net", true);
//        mailFilter.addServer("proxy.block.transip.nl", true);
//        mailFilter.addServer("orvedb.aupads.org", true);
//        mailFilter.addServer("spam.spamrats.com", true);
//        mailFilter.addServer("virus.rbl.msrbl.net", true);
//        mailFilter.addServer("dynip.rothen.com", true);
//        mailFilter.addServer("socks.dnsbl.sorbs.net", true);
//        mailFilter.addServer("smtp.dnsbl.sorbs.net", true);
//        mailFilter.addServer("ubl.unsubscore.com", true);
//        mailFilter.addServer("bl.spamcop.net", true);
//        mailFilter.addServer("relays.bl.kundenserver.de", true);
//        mailFilter.addServer("ips.backscatterer.org", true);
//        mailFilter.addServer("spamlist.or.kr", true);
//        mailFilter.addServer("bl.spamcannibal.org", true);
//        mailFilter.addServer("bl.emailbasura.org", true);
//        mailFilter.addServer("owfs.dnsbl.net.au", true);
//        mailFilter.addServer("ricn.dnsbl.net.au", true);
//        mailFilter.addServer("combined.rbl.msrbl.net", true);
//        mailFilter.addServer("wormrbl.imp.ch", true);
//        mailFilter.addServer("virbl.bit.nl", true);
//        mailFilter.addServer("db.wpbl.info", true);
//        mailFilter.addServer("ubl.lashback.com", true);
//        mailFilter.addServer("nlwhitelist.dnsbl.bit.nl", false);
//        mailFilter.addServer("virus.rbl.jp", true);
//        mailFilter.addServer("dnsbl.inps.de", true);
//        mailFilter.addServer("relays.nether.net", true);
//        mailFilter.addServer("dul.dnsbl.sorbs.net", true);
//        mailFilter.addServer("dyna.spamrats.com", true);
//        mailFilter.addServer("spam.abuse.ch", true);
//        mailFilter.addServer("ohps.dnsbl.net.au", true);
//        mailFilter.addServer("misc.dnsbl.sorbs.net", true);
//        mailFilter.addServer("dnsbl.ahbl.org", true);
//        mailFilter.addServer("torserver.tor.dnsbl.sectoor.de", true);
//        mailFilter.addServer("sa-accredit.habeas.com", false);
//        mailFilter.addServer("web.dnsbl.sorbs.net", true);
//        mailFilter.addServer("omrs.dnsbl.net.au", true);
//        mailFilter.addServer("xbl.spamhaus.org", true);
//        mailFilter.addServer("proxy.bl.gweep.ca", true);
//        mailFilter.addServer("whitelist.surriel.com", false);
//        mailFilter.addServer("dnsbl.sorbs.net", true);
//        mailFilter.addServer("sbl.spamhaus.org", true);
//        mailFilter.addServer("psbl.surriel.com", true);
//        mailFilter.addServer("ix.dnsbl.manitu.net", true);
//        mailFilter.addServer("dnsbl-3.uceprotect.net", true);
//        mailFilter.addServer("blacklist.woody.ch", true);
//        mailFilter.addServer("zombie.dnsbl.sorbs.net", true);
//        mailFilter.addServer("cbl.abuseat.org", true);
//        mailFilter.addServer("exemptions.ahbl.org", false);
//        mailFilter.addServer("rbl.interserver.net", true);
//        mailFilter.addServer("dnsbl.njabl.org", true);
//        mailFilter.addServer("be.whitelist.skopos.be", false);
//        mailFilter.addServer("dnsbl.cyberlogic.net", true);
//        mailFilter.addServer("wl.trusted-forwarder.org", false);
        Main.test(mailFilter, 0.001f, true);
        mailFilter.store();
    }
    
    private final class Server extends SimpleFilter {
        
        private final boolean OUTPUT;
        
        private Server(String host, boolean output) {
            super(host);
            OUTPUT = output;
        }
        
        private Server(ObjectInputStream ois) throws IOException, Exception {
            super(ois);
            OUTPUT = ois.readBoolean();
        }
        
        @Override
        public void store(ObjectOutputStream oos) throws IOException {
            super.store(oos);
            oos.writeBoolean(OUTPUT);
        }
        
        private boolean isOutputPositive() {
            return OUTPUT;
        }
        
        @Override
        public boolean equals(Object o) {
            if (o instanceof Server) {
                Server other = (Server) o;
                return this.getDescriptor().equals(other.getDescriptor());
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return getDescriptor().hashCode();
        }

        private synchronized boolean isListed(String ip) {
            StringTokenizer tokenizer = new StringTokenizer(ip, ".");
            String ipAddress = tokenizer.nextToken();
            ipAddress = tokenizer.nextToken() + "." + ipAddress;
            ipAddress = tokenizer.nextToken() + "." + ipAddress;
            ipAddress = tokenizer.nextToken() + "." + ipAddress;
            String query = ipAddress + "." + getDescriptor();
            Boolean listed = resultMap.get(query);
            if (listed == null) {
                long startTime = System.currentTimeMillis();
                try {
                    System.out.println("Consulting DNS " + query + ".");
                    Lookup lookup = new Lookup(query, Type.A);
                    Resolver resolver = new SimpleResolver();
                    resolver.setTimeout(TIMEOUT / 1000, TIMEOUT % 1000);
                    lookup.setResolver(resolver);
                    lookup.setCache(null);
                    lookup.run();
                    int result = lookup.getResult();
                    if (result == Lookup.SUCCESSFUL) {
                        resultMap.put(query, Boolean.TRUE);
                        return true;
                    } else if (result == Lookup.HOST_NOT_FOUND) {
                        resultMap.put(query, Boolean.FALSE);
                        return false;
                    } else if (result == Lookup.UNRECOVERABLE || result == Lookup.TRY_AGAIN) {
                        System.err.println("Network error for DNS " + query + ".");
                        return false;
                    } else {
                        return false;
                    }
                } catch (Exception exception) {
                    return false;
                } finally {
                    long endTime = System.currentTimeMillis();
                    addTimeSpent(endTime - startTime);
                }
            } else {
                return listed;
            }
        }
    }
}
