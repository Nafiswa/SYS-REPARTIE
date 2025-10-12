import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import jvn.JvnCoordImpl;

public class JvnCoordLauncher {
    public static void main(String[] args) {
        try {
            // Lire la config RMI depuis les propriétés système (avec valeurs par défaut)
            final String host = System.getProperty("jvn.registry.host", "127.0.0.1");
            final int port = Integer.getInteger("jvn.registry.port", 1099);

            // Forcer un hostname explicite pour éviter les soucis de résolution sur macOS/IPv6
            if (System.getProperty("java.rmi.server.hostname") == null) {
                System.setProperty("java.rmi.server.hostname", host);
            }

            // Créer le coordinateur
            JvnCoordImpl coordinator = new JvnCoordImpl();
            
            // Démarrer ou récupérer le registry RMI local
            try {
                LocateRegistry.createRegistry(port);
                System.out.println("RMI Registry démarré (in-process) sur " + host + ":" + port);
            } catch (ExportException e) {
                System.out.println("RMI Registry déjà démarré, on s'y connecte sur " + host + ":" + port + "...");
            }

            // L'enregistrer dans le registry RMI
            Registry registry = LocateRegistry.getRegistry(host, port);
            registry.rebind("JvnCoordinator", coordinator);
            
            System.out.println("COORDINATEUR: Prêt et en attente de connexions...");
            
            // Maintenir le coordinateur actif
            synchronized(JvnCoordLauncher.class) {
                JvnCoordLauncher.class.wait();
            }
            
        } catch (Exception e) {
            System.err.println("ERREUR Coordinateur: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}