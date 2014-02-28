/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.allchemistry.mailclassify.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 *
 * @author Leandro Carlos Rodrigues <leandro.carlos.rodrigues@gmail.com>
 */
public class Utilities {
    
    public static Serializable stringToObject(String objectString)
            throws IOException, ClassNotFoundException {
        byte [] data = Base64Coder.decode(objectString);
        ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(data));
        Serializable object = (Serializable) ois.readObject();
        ois.close();
        return object;
    }

    public static String objectToString(Serializable object) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            oos.close();
            return new String(Base64Coder.encode(baos.toByteArray()));
        } catch (IOException iOException) {
            return null;
        }
    }
    
    public static void storeObject(Serializable object,
            File file) throws FileNotFoundException, IOException {
        FileOutputStream fos = new FileOutputStream(file);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(object);
        oos.close();
    }
    
    public static Serializable loadObject(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Serializable object = (Serializable) ois.readObject();
            ois.close();
            return object;
        } catch (Exception exception) {
            return null;
        }
    }
    
    public static String simplify(String text) {
        int length = text.length();
        StringBuilder builder = new StringBuilder(length);
        boolean lastSpace = false;
        boolean lastBreak = true;
        for (char character : text.toCharArray()) {
            switch (character) {
                case 'a':
                case 'á':
                case 'à':
                case 'ã':
                case 'ä':
                case 'â':
                case 'å':
                case 'A':
                case 'Á':
                case 'À':
                case 'Ã':
                case 'Ä':
                case 'Â':
                case 'ª':
                    builder.append('a');
                    lastSpace = false;
                    lastBreak = false;
                    break;
                case 'b':
                case 'B':
                    builder.append('b');
                    lastSpace = false;
                    lastBreak = false;
                    break;
                case 'c':
                case 'ç':
                case 'C':
                case 'Ç':
                    builder.append('c');
                    lastSpace = false;
                    lastBreak = false;
                    break;
                case 'd':
                case 'D':
                    builder.append('d');
                    lastSpace = false;
                    lastBreak = false;
                    break;
                case 'e':
                case 'é':
                case 'è':
                case 'ë':
                case 'ê':
                case 'E':
                case 'É':
                case 'È':
                case 'Ë':
                case 'Ê':
                    builder.append('e');
                    lastSpace = false;
                    lastBreak = false;
                    break;
                case 'f':
                case 'F':
                    builder.append('f');
                    lastSpace = false;
                    lastBreak = false;
                    break;
                case 'g':
                case 'G':
                    builder.append('g');
                    lastSpace = false;
                    lastBreak = false;
                    break;
                case 'h':
                case 'H':
                    builder.append('h');
                    lastSpace = false;
                    lastBreak = false;
                    break;
                case 'i':
                case 'í':
                case 'ì':
                case 'ï':
                case 'î':
                case 'I':
                case 'Í':
                case 'Ì':
                case 'Ï':
                case 'Î':
                    builder.append('i');
                    lastSpace = false;
                    lastBreak = false;
                    break;
                case 'j':
                case 'J':
                    builder.append('j');
                    lastSpace = false;
                    lastBreak = false;
                    break;
                case 'k':
                case 'K':
                    builder.append('k');
                    lastSpace = false;
                    lastBreak = false;
                    break;
                case 'l':
                case 'L':
                    builder.append('l');
                    lastSpace = false;
                    lastBreak = false;
                    break;
                case 'm':
                case 'M':
                    builder.append('m');
                    lastSpace = false;
                    lastBreak = false;
                    break;
                case 'n':
                case 'ñ':
                case 'N':
                case 'Ñ':
                    builder.append('n');
                    lastSpace = false;
                    lastBreak = false;
                    break;
                case 'o':
                case 'ó':
                case 'ò':
                case 'õ':
                case 'ö':
                case 'ô':
                case 'ø':
                case 'O':
                case 'Ó':
                case 'Ò':
                case 'Õ':
                case 'Ö':
                case 'Ô':
                case 'º':
                case '°':
                    builder.append('o');
                    lastSpace = false;
                    lastBreak = false;
                    break;
                case 'p':
                case 'P':
                    builder.append('p');
                    lastSpace = false;
                    lastBreak = false;
                    break;
                case 'q':
                case 'Q':
                    builder.append('q');
                    lastSpace = false;
                    lastBreak = false;
                    break;
                case 'r':
                case 'R':
                    builder.append('r');
                    lastSpace = false;
                    lastBreak = false;
                    break;
                case 's':
                case 'S':
                    builder.append('s');
                    lastSpace = false;
                    lastBreak = false;
                    break;
                case 't':
                case 'T':
                    builder.append('t');
                    lastSpace = false;
                    lastBreak = false;
                    break;
                case 'u':
                case 'ú':
                case 'ù':
                case 'ü':
                case 'û':
                case 'U':
                case 'Ú':
                case 'Ù':
                case 'Ü':
                case 'Û':
                    builder.append('u');
                    lastSpace = false;
                    lastBreak = false;
                    break;
                case 'v':
                case 'V':
                    builder.append('v');
                    lastSpace = false;
                    lastBreak = false;
                    break;
                case 'w':
                case 'W':
                    builder.append('w');
                    lastSpace = false;
                    lastBreak = false;
                    break;
                case 'x':
                case 'X':
                    builder.append('x');
                    lastSpace = false;
                    lastBreak = false;
                    break;
                case 'y':
                case 'ý':
                case 'ÿ':
                case 'Y':
                case 'Ý':
                    builder.append('y');
                    lastSpace = false;
                    lastBreak = false;
                    break;
                case 'z':
                case 'Z':
                    builder.append('z');
                    lastSpace = false;
                    lastBreak = false;
                    break;
                case 'æ':
                    builder.append('a');
                    builder.append('e');
                    lastSpace = false;
                    lastBreak = false;
                    break;
                case '-':
                    break;
                case '.':
                case ',':
                case ';':
                case ':':
                case '!':
                case '?':
                case '"':
                case '\'':
                case '(':
                case ')':
                case '[':
                case ']':
                case '{':
                case '}':
                case '|':
                case '\t':
                    if (lastSpace) {
                        int index = builder.length() - 2;
                        builder.insert(index, '\n');
                        lastBreak = true;
                        lastSpace = false;
                    } else if (!lastBreak) {
                        builder.append('\n');
                        lastBreak = true;
                        lastSpace = false;
                    }
                    break;
                default:
                    if (!lastSpace && !lastBreak) {
                        builder.append(' ');
                        lastSpace = true;
                        lastBreak = false;
                    }
            }
        }
        String formated = builder.toString().trim();
        if (formated.isEmpty()) {
            return null;
        } else {
            return formated;
        }
    }
}
