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
        System.out.println("  r         - Lire le contenu");
        System.out.println("  r SECONDS - Lecture longue pendant SECONDS secondes");
        System.out.println("  w TEXT    - Écrire TEXT");
        System.out.println("  w TEXT SECONDS - Écriture longue avec verrou pendant SECONDS secondes");
        System.out.println("  obj NOUVEAU_NOM - Changer d'objet");
        System.out.println("  info      - Afficher info sur l'objet actuel");
        System.out.println("  q         - Quitter");
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
                            if (parts.length == 1) {
                                // Lecture simple
                                String content = sentence.read();
                                System.out.println("Contenu lu: '" + content + "'");
                            } else if (parts.length == 2) {
                                // Lecture longue avec durée
                                try {
                                    int seconds = Integer.parseInt(parts[1]);
                                    System.out.println("Début de la lecture longue (" + seconds + "s)...");
                                    String content = sentence.simulateLongReadOperation(seconds * 1000);
                                    System.out.println("Contenu lu (après " + seconds + "s): '" + content + "'");
                                } catch (NumberFormatException e) {
                                    System.out.println("Usage: r <secondes> (nombre entier)");
                                }
                            } else {
                                System.out.println("Usage: r [secondes]");
                            }
                            break;
                            
                        case "w":
                        case "write":
                            if (parts.length < 2) {
                                System.out.println("Usage: w <texte> [secondes]");
                                break;
                            }
                            
                            // Vérifier si le dernier argument est un nombre (durée)
                            String[] wParts = input.split("\\s+");
                            if (wParts.length >= 3) {
                                try {
                                    // Essayer de parser le dernier élément comme un nombre
                                    String lastPart = wParts[wParts.length - 1];
                                    int seconds = Integer.parseInt(lastPart);
                                    
                                    // Reconstituer le texte (tout sauf le dernier élément)
                                    StringBuilder textBuilder = new StringBuilder();
                                    for (int i = 1; i < wParts.length - 1; i++) {
                                        if (i > 1) textBuilder.append(" ");
                                        textBuilder.append(wParts[i]);
                                    }
                                    String longText = textBuilder.toString();
                                    
                                    // Écriture longue
                                    System.out.println("Début de l'écriture longue (" + seconds + "s)...");
                                    sentence.simulateLongWriteOperation(longText, seconds * 1000);
                                    System.out.println("Écriture longue terminée.");
                                    break;
                                } catch (NumberFormatException e) {
                                    // Le dernier élément n'est pas un nombre, traiter comme texte normal
                                }
                            }
                            
                            // Écriture simple
                            String text = input.substring(input.indexOf(' ') + 1);
                            sentence.write(text);
                            System.out.println("Texte écrit: '" + text + "'");
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