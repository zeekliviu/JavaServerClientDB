import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Scanner;

import static java.lang.System.exit;

public class Client {
    public static void main(String[] args)
    {
        int port = 8989;
        try {
            var adresa = InetAddress.getByName("localhost");
            var client = new Socket(adresa, port);
            var out = new ObjectOutputStream(client.getOutputStream());
            var in = new ObjectInputStream(client.getInputStream());
            var scaner = new Scanner(System.in);
            System.out.print("Numele dvs: ");
            var nume = scaner.nextLine();
            System.out.println("UTILIZARE: [exit|cumpar <nume_carte>|caut dupa titlu <nume_carte>|caut dupa autor <nume_autor>]");
            var reader = new BufferedReader(new InputStreamReader(System.in));
            while(true)
            {
                System.out.print("Introduceti comanda dorita: ");
                var mesaj = reader.readLine();
                mesaj = mesaj.replaceAll("\\s+", " ").trim(); // msj = ' ana    are  mere  ' -> msj = msj.replaceAll("\\s+", " ").trim() -> msj = 'ana are mere'
                TipOperatie operatie;
                Request request;
                StringBuilder query = new StringBuilder();
                if(mesaj.equals("exit"))
                {
                    System.out.println("Conexiune închisă.");
                    out.writeObject(new Request("exit", TipOperatie.IESIRE, nume));
                    in.close();
                    out.close();
                    client.close();
                    exit(0);
                }
                String[] mesajSplit = mesaj.split(" ");
                for(int i=0; i< mesajSplit.length; i++)
                    mesajSplit[i] = mesajSplit[i].toLowerCase();
                if(mesajSplit[0].equals("cumpar"))
                {
                    operatie = TipOperatie.CUMPAR;
                    for(int i=1; i<mesajSplit.length; i++)
                        query.append(mesajSplit[i]).append(" ");
                    query.deleteCharAt(query.length()-1);
                    request = new Request(query.toString(), operatie, nume);
                }
                else if (mesajSplit[0].equals("caut"))
                {
                    var by = mesajSplit[1] + " " + mesajSplit[2];
                    if(by.equals("dupa titlu")) {
                        operatie = TipOperatie.CAUT_TITLU;
                        for (int i = 3; i < mesajSplit.length; i++)
                            query.append(mesajSplit[i]).append(" ");
                        query.deleteCharAt(query.length()-1);
                        request = new Request(query.toString(), operatie, nume);
                    }
                    else if(by.equals("dupa autor"))
                    {
                        operatie = TipOperatie.CAUT_AUTOR;
                        for (int i = 3; i < mesajSplit.length; i++)
                            query.append(mesajSplit[i]).append(" ");
                        query.deleteCharAt(query.length()-1);
                        request = new Request(query.toString(), operatie, nume);
                    }
                    else {
                        operatie = TipOperatie.NEDEFINIT;
                        request = new Request(query.toString(), operatie, nume);
                    }
                } else if (mesajSplit[0].equals("returnez")) {
                    operatie = TipOperatie.RETURNEZ;
                    for (int i = 1; i < mesajSplit.length; i++)
                        query.append(mesajSplit[i]).append(" ");
                    query.deleteCharAt(query.length()-1);
                    if(!new File("./"+"Cărțile lui "+nume).exists())
                    {
                        System.out.println("Nu ati imprumutat nicio carte.");
                        continue;
                    } else if (!new File("./"+"Cărțile lui "+nume+"/"+ query +".pdf").exists()) {
                        System.out.println("Nu ati imprumutat cartea " + query + ".");
                        continue;
                    }
                    request = new Request(query.toString(), operatie, nume);
                } else {
                    operatie = TipOperatie.NEDEFINIT;
                    request = new Request(query.toString(), operatie, nume);
                }
                out.writeObject(request);
                var raspuns = in.readObject();
                if(raspuns instanceof String raspunsString)
                {
                    System.out.println(raspunsString);
                    if(raspunsString.equals("Cartea a fost returnată cu succes. Mulțumim!"))
                        new File("./"+"Cărțile lui "+nume+"/"+ query +".pdf").delete();
                }
                else if(raspuns instanceof String[] raspunsMasivString)
                {
                    for(var s : raspunsMasivString)
                        System.out.println(s);
                }
                else if(raspuns instanceof Response serverResponse)
                {
                    // creeaza directorul daca nu exista
                    File directory = new File("./"+"Cărțile lui "+nume);
                    if (!directory.exists()) {
                        directory.mkdirs();
                    }
                    try (FileOutputStream fos = new FileOutputStream("./"+"Cărțile lui "+nume+"/"+serverResponse.getNumeCarte()+".pdf")) {
                        fos.write(serverResponse.getContinut());
                        System.out.println("Carte salvata la: " + Path.of("Cărțile lui "+nume+"/"+serverResponse.getNumeCarte()+".pdf").toAbsolutePath());
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
