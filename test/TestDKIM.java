
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import org.apache.james.jdkim.DKIMVerifier;
import org.apache.james.jdkim.api.SignatureRecord;
import org.apache.james.jdkim.exceptions.PermFailException;
import org.apache.james.jdkim.exceptions.TempFailException;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Leandro Carlos Rodrigues <leandro.carlos.rodrigues@gmail.com>
 */
public class TestDKIM {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        DKIMVerifier verifier = new DKIMVerifier();
        File hamFolder = new File("./examples/ham/");
        for (File file : hamFolder.listFiles()) {
            FileInputStream inputStream = new FileInputStream(file);
            try {
                List<SignatureRecord> list = verifier.verify(inputStream);
                if (list != null && !list.isEmpty()) {
                    System.out.println(file);
                    for (SignatureRecord record : list) {
                        System.out.println(record);
                    }
                }
            } catch (TempFailException exception) {
                System.out.println(file);
                exception.printStackTrace();
            } catch (PermFailException exception) {
                System.out.println(file);
                exception.printStackTrace();
            }
        }
    }
}
