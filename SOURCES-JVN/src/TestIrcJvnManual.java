import java.util.Scanner;
import jvn.JvnException;
import jvn.JvnServerImpl;

/**
 * Test interactif pour Javanaise - un seul client
 */
public class TestIrcJvnManual {
    public static void main(String[] args) {
        String clientName = System.getProperty("clientName", "Client");
        String objectName = args.length > 0 ? args[0] : "IRC_SENTENCE";
        
        System.out.println("=== " + clientName + " Javanaise Interactif ===");
        System.out.println("Objet JVN: " + objectName);
        System.out.println("Commandes disponibles:");
        System.out.println("  r      - Lire le contenu");
        System.out.println("  w TEXT - Écrire TEXT");
        System.out.println("  long TEXT SECONDS - Écriture longue avec verrou pendant SECONDS secondes");
        System.out.println("  obj NOUVEAU_NOM - Changer d'objet");
        System.out.println("  info   - Afficher info sur l'objet actuel");
        System.out.println("  q      - Quitter");
        System.out.println();
        
        try {
            irc.SentenceJvnCustom sentence = new irc.SentenceJvnCustom(objectName);
            Scanner scanner = new Scanner(System.in);
            
            while (true) {
                System.out.print(clientName + "> ");
                String input = scanner.nextLine().trim();
                
                if (input.isEmpty()) continue;
                
                String[] parts = input.split(" ", 3);
                String command = parts[0].toLowerCase();
                
                try {
                    switch (command) {
                        case "q":
                        case "quit":
                            System.out.println("Au revoir !");
                            return;
                            
                        case "r":
                        case "read":
                            String content = sentence.read();
                            System.out.println("Contenu lu: '" + content + "'");
                            break;
                            
                        case "w":
                        case "write":
                            if (parts.length < 2) {
                                System.out.println("Usage: w <texte>");
                                break;
                            }
                            String text = input.substring(input.indexOf(' ') + 1);
                            sentence.write(text);
                            System.out.println("Texte écrit: '" + text + "'");
                            break;
                            
                        case "long":
                            // Parser différemment pour gérer les guillemets
                            String[] longParts = input.split("\\s+");
                            if (longParts.length < 3) {
                                System.out.println("Usage: long <texte> <secondes>");
                                System.out.println("Exemple: long Hello 10");
                                System.out.println("Exemple: long \"Hello world\" 10");
                                break;
                            }
                            
                            // Trouver le dernier élément comme nombre de secondes
                            String secondsStr = longParts[longParts.length - 1].replaceAll("['\"]", "");
                            
                            // Reconstituer le texte (tout sauf le dernier élément)
                            StringBuilder textBuilder = new StringBuilder();
                            for (int i = 1; i < longParts.length - 1; i++) {
                                if (i > 1) textBuilder.append(" ");
                                textBuilder.append(longParts[i]);
                            }
                            String longText = textBuilder.toString().replaceAll("^\"|\"$|^'|'$", "");
                            
                            int seconds = Integer.parseInt(secondsStr);
                            System.out.println("Début de l'écriture longue (" + seconds + "s)...");
                            sentence.simulateLongWriteOperation(longText, seconds * 1000);
                            System.out.println("Écriture longue terminée.");
                            break;
                            
                        case "obj":
                        case "object":
                            if (parts.length < 2) {
                                System.out.println("Usage: obj <nom_objet>");
                                break;
                            }
                            String newObjectName = parts[1];
                            System.out.println("Changement vers l'objet: " + newObjectName);
                            sentence = new irc.SentenceJvnCustom(newObjectName);
                            System.out.println("Maintenant connecté à l'objet: " + sentence.getObjectName());
                            break;
                            
                        case "info":
                            System.out.println("=== Informations Objet ===");
                            System.out.println("Nom: " + sentence.getObjectName());
                            System.out.println("ID: " + sentence.getObjectId());
                            System.out.println("Client: " + clientName);
                            break;
                            
                        default:
                            System.out.println("Commande inconnue: " + command);
                            break;
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Erreur: nombre invalide");
                } catch (JvnException e) {
                    System.out.println("Erreur JVN: " + e.getMessage());
                } catch (Exception e) {
                    System.out.println("Erreur: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            System.err.println("Erreur fatale: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                JvnServerImpl.jvnGetServer().jvnTerminate();
            } catch (Exception e) {
                // Ignorer les erreurs de terminaison
            }
        }
    }
}