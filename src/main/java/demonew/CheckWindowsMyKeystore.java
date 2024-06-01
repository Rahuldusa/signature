package demonew;

import java.security.KeyStore;
import java.util.Enumeration;

public class CheckWindowsMyKeystore {
    public static void main(String[] args) {
        try {
            KeyStore ks = KeyStore.getInstance("Windows-MY");
            ks.load(null, null);

            Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                System.out.println("Alias: " + aliases.nextElement());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
