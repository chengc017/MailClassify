/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.allchemistry.mailclassify.filter;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeMap;
import javax.mail.Message;
import br.com.allchemistry.mailclassify.Main;
import static br.com.allchemistry.mailclassify.filter.ClassifyFilter.OUTPUT_INCONCLUSIVE;
import static br.com.allchemistry.mailclassify.filter.ClassifyFilter.OUTPUT_NEGATIVE;
import static br.com.allchemistry.mailclassify.filter.ClassifyFilter.OUTPUT_POSITIVE;

/**
 *
 * @author Leandro Carlos Rodrigues <leandro.carlos.rodrigues@gmail.com>
 */
public class ListFilter extends ClassifyFilter {

    private ArrayList<ClassifyFilter>[] filterLists;
    
    public ListFilter(String descriptor, int size) throws Exception {
        super(descriptor);
        filterLists = new ArrayList[size];
        for (int i = 0; i < size; i++) {
            filterLists[i] = new ArrayList<ClassifyFilter>();
        }
    }
    
    public final void addFilter(ClassifyFilter filter, int index) throws Exception {
        if (filter == null) {
            throw new Exception("Invalid mail filter.");
        } else {
            filterLists[index].add(filter);
        }
    }
    
    @Override
    public final byte trainingNegative(Message message) {
        long startTime = System.currentTimeMillis();
        byte output = OUTPUT_INCONCLUSIVE;
        for (ArrayList<ClassifyFilter> filterList : filterLists) {
            for (ClassifyFilter filter : filterList) {
                if (filter.getHitRatio() > Math.random()) {
                    output = filter.trainingNegative(message);
                    if (output != OUTPUT_INCONCLUSIVE) {
                        String after = filterList.toString();
                        Collections.sort(filterList);
                        String before = filterList.toString();
                        if (!after.equals(before)) {
                            System.out.println(filterList);
                        }
                        break;
                    }
                }
            }
            if (output != OUTPUT_INCONCLUSIVE) {
                break;
            }
        }
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

    @Override
    public final byte trainingPositive(Message message) {
        long startTime = System.currentTimeMillis();
        byte output = OUTPUT_INCONCLUSIVE;
        for (ArrayList<ClassifyFilter> filterList : filterLists) {
            for (ClassifyFilter filter : filterList) {
                if (filter.getHitRatio() > Math.random()) {
                    output = filter.trainingPositive(message);
                    if (output != OUTPUT_INCONCLUSIVE) {
                        String after = filterList.toString();
                        Collections.sort(filterList);
                        String before = filterList.toString();
                        if (!after.equals(before)) {
                            System.out.println(filterList);
                        }
                        break;
                    }
                }
            }
            if (output != OUTPUT_INCONCLUSIVE) {
                break;
            }
        }
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

    @Override
    public final byte classify(Message message) {
        long startTime = System.currentTimeMillis();
        byte output = OUTPUT_INCONCLUSIVE;
        for (ArrayList<ClassifyFilter> filterList : filterLists) {
            for (ClassifyFilter filter : filterList) {
                if (filter.getHitRatio() > Math.random()) {
                    output = filter.classify(message);
                    if (output == OUTPUT_NEGATIVE) {
                        addNegativeOutput();
                        break;
                    } else if (output == OUTPUT_POSITIVE) {
                        addPositiveOutput();
                        break;
                    }
                }
            }
            if (output != OUTPUT_INCONCLUSIVE) {
                break;
            }
        }
        if (output == OUTPUT_INCONCLUSIVE) {
            addInconclusiveOutput();
        }
        long endTime = System.currentTimeMillis();
        addTimeSpent(endTime - startTime);
        return output;
    }
    
    @Override
    public void printStatistics(PrintStream printStream) {
        super.printStatistics(printStream);
        float volume = 1.0f;
        for (ArrayList<ClassifyFilter> filterList : filterLists) {
            for (ClassifyFilter filter : filterList) {
                printStream.println();
                printStream.println("Received volume: " + volume);
                filter.printStatistics(printStream);
                volume *= filter.getInconclusiveRatio();
            }
        }
    }
    
    @Override
    public void store() throws IOException {
        for (ArrayList<ClassifyFilter> filterList : filterLists) {
            for (ClassifyFilter filter : filterList) {
                try {
                    filter.store();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }
        }
    }
    
    @Override
    public void downsizeCache(float ratio) {
        for (ArrayList<ClassifyFilter> filterList : filterLists) {
            for (ClassifyFilter filter : filterList) {
                filter.downsizeCache(ratio);
            }
        }
        System.gc();
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        ListFilter mailFilter = new ListFilter("Filter List", 1);
        try {
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
            Main.test(mailFilter, 1.0f, true);
        } finally {
            mailFilter.store();
        }
    }
}
