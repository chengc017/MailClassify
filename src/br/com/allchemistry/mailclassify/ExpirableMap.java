/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.allchemistry.mailclassify;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import br.com.allchemistry.mailclassify.distribution.BinomialDistribution;

/**
 *
 * @param <K> the key
 * @param <V> the value
 * @author Leandro Carlos Rodrigues <leandro.carlos.rodrigues@gmail.com>
 */
public class ExpirableMap<K,V> {
    
    // Expiration time in milliseconds.
    private long BEGIN_TIME;
    private final int EXPIRATION_TIME;
    
    private final HashMap<K,Register> registerMap = new HashMap<K,Register>();
    
    /**
     *
     * @param max maximun elements on map.
     */
    public ExpirableMap() {
        EXPIRATION_TIME = 0;
        BEGIN_TIME = System.currentTimeMillis();
    }
    
    /**
     *
     * @param max maximun elements on map.
     * @param expiration time to expire value in days and zero for never expires.
     */
    public ExpirableMap(int expiration) {
        EXPIRATION_TIME = expiration * 86400000;
        BEGIN_TIME = System.currentTimeMillis();
    }
    
    public ExpirableMap(ObjectInputStream ois) throws IOException, Exception {
        int version = ois.readInt();
        if (version == 1) {
            BEGIN_TIME = ois.readLong();
            EXPIRATION_TIME = ois.readInt();
            int SIZE_MAX = ois.readInt();
            int SIZE_MIN = ois.readInt();
            int size = ois.readInt();
            for (int i = 0; i < size; i++) {
                try {
                    K key = (K) ois.readObject();
                    Register register = new Register(ois, version);
                    registerMap.put(key, register);
                } catch (ClassNotFoundException exception) {
                    exception.printStackTrace();
                }
            }
        } else if (version == 2) {
            BEGIN_TIME = ois.readLong();
            EXPIRATION_TIME = ois.readInt();
            int size = ois.readInt();
            for (int i = 0; i < size; i++) {
                try {
                    K key = (K) ois.readObject();
                    Register register = new Register(ois, version);
                    registerMap.put(key, register);
                } catch (ClassNotFoundException exception) {
                    exception.printStackTrace();
                }
            }
        } else if (version == 3) {
            BEGIN_TIME = ois.readLong();
            EXPIRATION_TIME = ois.readInt();
            int size = ois.readInt();
            for (int i = 0; i < size; i++) {
                try {
                    K key = (K) ois.readObject();
                    Register register = new Register(ois, version);
                    registerMap.put(key, register);
                } catch (ClassNotFoundException exception) {
                    exception.printStackTrace();
                }
            }
        } else if (version == 4) {
            BEGIN_TIME = ois.readLong();
            EXPIRATION_TIME = ois.readInt();
            int size = ois.readInt();
            for (int i = 0; i < size; i++) {
                try {
                    String className = ois.readUTF();
                    K key = null;
                    if (className.equals("mailclassify.CIDR")) {
                        key = (K) new CIDR(ois);
                    } else {
                        key = (K) ois.readObject();
                    }
                    Register register = new Register(ois, version);
                    registerMap.put(key, register);
                } catch (ClassNotFoundException exception) {
                    exception.printStackTrace();
                }
            }
        } else {
            throw new IOException("Version not implemented.");
        }
    }
    
    public void store(ObjectOutputStream oos) throws IOException {
        oos.writeInt(4); // Version
        oos.writeLong(BEGIN_TIME);
        oos.writeInt(EXPIRATION_TIME);
        oos.writeInt(registerMap.size());
        for (K key : registerMap.keySet()) {
            String className = key.getClass().getCanonicalName();
            oos.writeUTF(className);
            if (className.equals("mailclassify.CIDR")) {
                CIDR cidr = (CIDR) key;
                cidr.store(oos);
            } else {
                oos.writeObject(key);
            }
            Register register = registerMap.get(key);
            register.store(oos);
        }
    }
    
    public synchronized V put(K key, V value) throws Exception {
        if (key == null) {
            throw new Exception("Invalid key.");
        } else if (value == null) {
            throw new Exception("Invalid value.");
        } else {
            registerMap.put(key, new Register(value));
            return value;
        }
    }
    
    public synchronized V get(K key) {
        Register register = registerMap.get(key);
        if (register == null) {
            return null;
        } else if (register.isExpired()) {
            registerMap.remove(key);
            return null;
        } else {
            return register.getValue();
        }
    }
    
    public Collection<V> values() {
        Collection<Register> registers = registerMap.values();
        HashSet<V> values = new HashSet<V>(registers.size());
        for (Register register : registers) {
            if (!register.isExpired()) {
                V value = register.getValue();
                values.add(value);
            }
        }
        return values;
    }
    
    public boolean containsKey(K key) {
        return registerMap.containsKey(key);
    }
    
    public Set<K> keySet() {
        return registerMap.keySet();
    }
    
    public synchronized void clearExpired() {
        if (EXPIRATION_TIME > 0) {
            long newBeginTime = System.currentTimeMillis();
            int timeDiff = (int) (newBeginTime - BEGIN_TIME);
            BEGIN_TIME = newBeginTime;
            TreeSet<K> expiredSet = new TreeSet<K>();
            for (K key : registerMap.keySet()) {
                Register register = registerMap.get(key);
                register.first_query -= timeDiff;
                register.last_query -= timeDiff;
                if (register.isExpired()) {
                    expiredSet.add(key);
                }
            }
            for (K key : expiredSet) {
                registerMap.remove(key);
            }
        }
    }
    
    public void downsize(float ratio) throws Exception {
        if (ratio <= 0 || ratio >= 1) {
            throw new Exception("Invalid ratio.");
        } else {
            downsize((int) (registerMap.size() * ratio));
        }
    }
    
    public void downsize(int size) throws Exception {
        if (size <= 0) {
            throw new Exception("Invalid size.");
        } else {
            TreeMap<Integer,TreeSet<K>> timeCountMap =
                    new TreeMap<Integer,TreeSet<K>>();
            for (K key : registerMap.keySet()) {
                Register register = registerMap.get(key);
                int time = register.last_query;
                TreeSet<K> keySet;
                if (timeCountMap.containsKey(time)) {
                    keySet = timeCountMap.get(time);
                } else {
                    keySet = new TreeSet<K>();
                }
                keySet.add(key);
                timeCountMap.put(time, keySet);
            }

            for (int time : timeCountMap.keySet()) {
                for (K key : timeCountMap.get(time)) {
                    registerMap.remove(key);
                }
                if (registerMap.size() <= size) {
                    break;
                }
            }
        }
    }
    
    @Override
    public String toString() {
        return registerMap.toString();
    }
    
    private class Register {
        
        private int first_query;
        private int last_query;
        private V value;
        
        public Register(V value) throws Exception {
            setValue(value);
            first_query = (int) (System.currentTimeMillis() - BEGIN_TIME);
            last_query = (int) (System.currentTimeMillis() - BEGIN_TIME);
        }
        
        private Register(ObjectInputStream ois, int version)
                throws IOException, ClassNotFoundException, Exception {
            if (version == 1) {
                first_query = ois.readInt();
                last_query = ois.readInt();
                value = (V) ois.readObject();
            } else if (version == 2) {
                first_query = ois.readInt();
                last_query = ois.readInt();
                value = (V) ois.readObject();
            } else if (version == 3) {
                first_query = ois.readInt();
                last_query = ois.readInt();
                String className = ois.readUTF();
//                if (className.equals("mailclassify.filter.WhoisFilter.Result")) {
//                    value = (V) new WhoisFilter.Result(ois);
//                } else {
                    value = (V) ois.readObject();
//                }
             } else if (version == 4) {
                first_query = ois.readInt();
                last_query = ois.readInt();
                String className = ois.readUTF();
//                if (className.equals("mailclassify.filter.WhoisFilter.Result")) {
//                    value = (V) new WhoisFilter.Result(ois);
//                } else
                if (className.equals("mailclassify.CIDR")) {
                    value = (V) new CIDR(ois);
                } else if (className.equals("mailclassify.distribution.BinomialDistribution")) {
                    value = (V) new BinomialDistribution(ois);
                } else {
                    value = (V) ois.readObject();
                }
           }
        }
        
        public void store(ObjectOutputStream oos) throws IOException {
            oos.writeInt(first_query);
            oos.writeInt(last_query);
            String className = value.getClass().getCanonicalName();
            oos.writeUTF(className);
//            if (className.equals("mailclassify.filter.WhoisFilter.Result")) {
//                WhoisFilter.Result result = (WhoisFilter.Result) value;
//                result.store(oos);
//            } else
            if (className.equals("mailclassify.CIDR")) {
                CIDR cidr = (CIDR) value;
                cidr.store(oos);
            } else if (className.equals("mailclassify.distribution.BinomialDistribution")) {
                BinomialDistribution bd = (BinomialDistribution) value;
                bd.store(oos);
            } else {
                oos.writeObject(value);
            }
        }
        
        private void setValue(V value) throws Exception {
            if (value == null) {
                throw new Exception("Invalid value.");
            } else {
                this.value = value;
            }
        }
        
        public V getValue() {
            last_query = (int) (System.currentTimeMillis() - BEGIN_TIME);
            return value;
        }
        
        public boolean isExpired() {
            if (EXPIRATION_TIME == 0) {
                return false;
            } else {
                long expirationTime = first_query + BEGIN_TIME + EXPIRATION_TIME;
                long currentTime = System.currentTimeMillis();
                return expirationTime < currentTime;
            }
        }
        
        @Override
        public String toString() {
            return value.toString();
        }
    }
}
