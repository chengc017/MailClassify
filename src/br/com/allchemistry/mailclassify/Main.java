/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.allchemistry.mailclassify;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeMap;
import javax.mail.internet.MimeMessage;
import br.com.allchemistry.mailclassify.filter.*;

/**
 *
 * @author Leandro Carlos Rodrigues <leandro.carlos.rodrigues@gmail.com>
 */
public class Main {
    
    public static void main(String[] args) throws Exception {
        ListFilter mailFilter = new ListFilter("Filter List", 1);
        try {
            // Filter initialization.
            System.out.println("Loading filter cache.");
            mailFilter.addFilter(FromToFilter.loadOrCreate("fromto"), 0);
            mailFilter.addFilter(AddressFilter.loadOrCreate("fromaddress", "From"), 0);
            mailFilter.addFilter(DomainFilter.loadOrCreate("fromdomain", "From"), 0);
            mailFilter.addFilter(PhraseFilter.loadOrCreate("contentsubject", "Subject"), 0);
            mailFilter.addFilter(IpFilter.loadOrCreate("senderip", "Received"), 0);
            mailFilter.addFilter(HostFilter.loadOrCreate("senderhost", "Received"), 0);
            mailFilter.addFilter(DomainFilter.loadOrCreate("senderdomain", "Received"), 0);
            mailFilter.addFilter(PhraseFilter.loadOrCreate("contentbody"), 0);
            mailFilter.addFilter(SpfFilter.loadOrCreate("spf"), 0);
            mailFilter.addFilter(DkimFilter.loadOrCreate("dkim"), 0);
            ReputationFilter reputationFilter = ReputationFilter.loadOrCreate("reputation", 500);
            TreeMap<String,Boolean> serverMap = new TreeMap<String,Boolean>();
            serverMap.put("list.dnswl.org", false);
            serverMap.put("zen.spamhaus.org", true);
            serverMap.put("b.barracudacentral.org", true);
            serverMap.put("wl.nszones.com", false);
            reputationFilter.setServers(serverMap);
            mailFilter.addFilter(reputationFilter, 0);
            CidrFilter netownerFilter = CidrFilter.loadOrCreate("netowner");
            ArrayList<String> fieldList = new ArrayList<String>();
            fieldList.add("ownerid");
            fieldList.add("OrgId");
            fieldList.add("organisation");
            fieldList.add("descr");
            fieldList.add("network:Organization;I");
            fieldList.add("network:organization;I");
            fieldList.add("network:Organization");
            fieldList.add("network:Org-Name;I");
            fieldList.add("network:Org-Name");
            fieldList.add("network:org-name");
            fieldList.add("network:OrgName");
            netownerFilter.setFields(fieldList);
            mailFilter.addFilter(netownerFilter, 0);
            mailFilter.addFilter(DateFilter.loadOrCreate("date", 72), 0);
            mailFilter.addFilter(TimeFilter.loadOrCreate("hour"), 0);
            mailFilter.addFilter(WeekFilter.loadOrCreate("weekday"), 0);
            
            // Server socket.
            System.out.println("Starting server.");
            boolean listen = true;
            ServerSocket server = new ServerSocket(4444);
            while (listen) {                
                System.out.println("Server is listenning.");
                Socket socket = server.accept();
                try {
                    System.out.println("New connection.");
                    InputStream inputStream = socket.getInputStream();
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader reader = new BufferedReader(inputStreamReader);
                    String line = reader.readLine();
                    System.out.println(line);
                    if (line.startsWith("SHUTDOWN")) {
                        System.out.println("Shutting down server.");
                        listen = false;
                    } else if (line.startsWith("STORE")) {
                        System.out.println("Storing filter cache.");
                        mailFilter.store();
                        System.out.println("Filter cache stored.");
                    } else if (line.startsWith("CHECK ") || line.startsWith("REPORT ")) {
                        boolean report = line.startsWith("REPORT ");
                        line = reader.readLine();
                        System.out.println(line);
                        if (line.startsWith("Content-length: ")) {
                            int length = Integer.parseInt(line.substring(16));
                            System.out.println("Data length: " + length + " bytes.");
                            line = reader.readLine();
                            System.out.println(line);
                            if (line.startsWith("User: ")) {
                                line = reader.readLine();
                                if (line.length() == 0) {
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream(length);
                                    while (length > 0) {
                                        int code = reader.read();
                                        baos.write(code);
                                        length--;
                                    }
                                    baos.flush();
                                    System.out.println(baos.toString());
                                    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                                    baos.close();
                                    MimeMessage message = new MimeMessage(null, bais);
                                    byte output = mailFilter.classify(message);
                                    bais.close();
                                    String response = (output == ClassifyFilter.OUTPUT_POSITIVE ? "True" : "False");
                                    System.out.println("SPAMD/1.1 0 EX_OK");
                                    System.out.println("Spam: " + response + " ; " + output + " / 0");
                                    
                                    OutputStream outputStream = socket.getOutputStream();
                                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
                                    BufferedWriter writer = new BufferedWriter(outputStreamWriter);
                                    writer.write("SPAMD/1.1 0 EX_OK\r\n");
                                    writer.write("Spam: " + response + " ; " + output + " / 0\r\n");
                                    
                                    if (report) {
                                        System.out.println();
                                        System.out.println(mailFilter);
                                        writer.write("\r\n");
                                        writer.write(mailFilter + "\r\n");
                                    }
                                    writer.close();
                                    outputStreamWriter.close();
                                    outputStream.close();
                                } else {
                                    System.out.println(line);
                                    System.err.println("Unrecognized command.");
                                }
                            }
                        } else {
                            System.err.println("Unrecognized command.");
                        }
                    } else {
                        System.err.println("Unrecognized command.");
                    }
                    reader.close();
                    inputStreamReader.close();
                    inputStream.close();
                } finally {
                    socket.close();
                }
            }
        } finally {
            System.out.println("Storing filter cache.");
            mailFilter.store();
            System.out.println("Filter cache stored.");
        }
    }

    public static void test(ClassifyFilter mailFilter, float totalProp, boolean training) throws Exception {
        Runtime runtime = Runtime.getRuntime();
        File hamFolder = new File("./examples/ham/");
        File spamFolder = new File("./examples/spam/");
        LinkedList<File> hamList = new LinkedList<File>();
        LinkedList<File> spamList = new LinkedList<File>();
        for (File file : hamFolder.listFiles()) {
            if (Math.random() < totalProp) {
                hamList.add(file);
            }
        }
        for (File file : spamFolder.listFiles()) {
            if (Math.random() < totalProp) {
                spamList.add(file);
            }
        }
        int negative_false = 0;
        int negative_true = 0;
        int positive_false = 0;
        int positive_true = 0;
        int count = 0;
        long startTime = System.currentTimeMillis();
        long endTime;

        try {
            while (!hamList.isEmpty() && !spamList.isEmpty()) {
                int hamSize = hamList.size();
                int spamSize = spamList.size();
                double p = (double) hamSize / (hamSize + spamSize);
                double r = Math.random();
                if (r < p) {
                    File file = hamList.pollFirst();
                    FileInputStream inputStream = new FileInputStream(file);
                    MimeMessage message = new MimeMessage(null, inputStream);
                    byte output = mailFilter.trainingNegative(message);
                    if (output == PatternFilter.OUTPUT_NEGATIVE) {
                        negative_true++;
                    } else if (output == PatternFilter.OUTPUT_POSITIVE) {
                        positive_false++;
                    }
                } else {
                    File file = spamList.pollFirst();
                    FileInputStream inputStream = new FileInputStream(file);
                    MimeMessage message = new MimeMessage(null, inputStream);
                    byte output = mailFilter.trainingPositive(message);
                    if (output == PatternFilter.OUTPUT_POSITIVE) {
                        positive_true++;
                    } else if (output == PatternFilter.OUTPUT_NEGATIVE) {
                        negative_false++;
                    }
                }
                count++;
                endTime = System.currentTimeMillis();
                long time = endTime - startTime;
                int more = hamSize + spamSize - 1;
                if (time > 180000) {
                    float avgTime = (float) time / count;
                    float left = avgTime * more / 60000;
                    System.out.println(left + " minutes left.");
                    startTime = System.currentTimeMillis();
                    count = 0;
                    double freeMemory = (double) runtime.freeMemory() / runtime.maxMemory();
                    if (freeMemory < 0.0625d) {
                        System.out.println("Memory almost full.");
                        mailFilter.downsizeCache(0.9375f);
                    }
                    if (training) {
                        mailFilter.store();
                        System.out.println("Cache stored.");
                    }
                    
                    double eficience = (double) (negative_true + positive_true)
                    / (negative_false + negative_true + positive_false + positive_true);
                    PrintStream printStream = new PrintStream("./results.txt");
                    printStream.println("External eficience: " + eficience);
                    printStream.println();
                    mailFilter.printStatistics(printStream);
                }
            }
        } finally {
            double eficience = (double) (negative_true + positive_true)
                    / (negative_false + negative_true + positive_false + positive_true);

            System.out.println("External eficience: " + eficience);
            System.out.println();
            mailFilter.printStatistics(System.out);

            PrintStream printStream = new PrintStream("./results.txt");
            printStream.println("External eficience: " + eficience);
            printStream.println();
            mailFilter.printStatistics(printStream);
        }
    }
}
