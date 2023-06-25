import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class ServerThread extends Thread {
    private Socket client;
    private Connection conn;
    private static int nrClienti = 0;

    private static Map<String, SocketAddress> clienti = new HashMap<>();
    public ServerThread(Socket client, Connection conn) {
        super(String.format("%d", ++nrClienti));
        this.client = client;
        this.conn = conn;
    }

    @Override
    public void run() {
        try
        {
            System.out.println("Conexiune acceptată de la clientul cu numarul " +getName()+ " avand adresa "+client.getRemoteSocketAddress()+" la " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
            var in = new ObjectInputStream(client.getInputStream());
            var out = new ObjectOutputStream(client.getOutputStream());
            while(true)
            {
                Request request = (Request) in.readObject();
                if(clienti.containsKey(request.getName()))
                {
                    if(!clienti.get(request.getName()).equals(client.getRemoteSocketAddress()))
                    {
                        out.writeObject("Exista deja un client cu acest nume.");
                        continue;
                    }
                }
                else clienti.put(request.getName(), client.getRemoteSocketAddress());
                switch (request.getOperation())
                {
                    case CUMPAR:
                        try(var stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY)) {
                            var rs = stmt.executeQuery("SELECT * FROM admin.books WHERE lower(name) = '" + request.getQuery() + "'");
                            if(rs.next())
                            {
                                if(rs.getInt("stock")>0)
                                {
                                    try(var stmt2 = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY))
                                    {stmt2.executeUpdate("UPDATE admin.books SET stock = stock - 1 WHERE lower(name) = '" + request.getQuery() + "'");}
                                    catch (SQLException e) {
                                        throw new RuntimeException(e);
                                    }
                                    var numeCarte = rs.getString("name");
                                    var pdf = rs.getBytes("pdf");
                                    var response = new Response(numeCarte, pdf);
                                    out.writeObject(response);
                                    System.out.println("Clientul " + client.getRemoteSocketAddress() + " a cumpărat cartea \"" + numeCarte + "\" la " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
                                }
                                else {
                                    out.writeObject("Nu mai există stoc pentru această carte.");
                                    System.out.println("Clientul " + client.getRemoteSocketAddress() + " a încercat să cumpere cartea \"" + rs.getString("name") + "\" pentru care nu mai există stoc la " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
                                }
                            }
                            else
                                out.writeObject("Nu există nicio carte cu acest titlu.");
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    case CAUT_TITLU:
                        try(var stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY)) {
                            var rs = stmt.executeQuery("SELECT * FROM admin.books WHERE lower(name) = '" + request.getQuery() + "'");
                            if(rs.next())
                            {
                                var numeCarte = rs.getString("name");
                                var nrBuc = rs.getInt("stock");
                                out.writeObject("Cartea " + numeCarte + " are " + nrBuc + " bucăți în stoc.");
                                System.out.println("Clientul " + client.getRemoteSocketAddress() + " a căutat cartea \"" + numeCarte + "\" la " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
                            }
                            else
                            {
                                out.writeObject("Nu există nicio carte cu acest titlu.");
                                System.out.println("Clientul " + client.getRemoteSocketAddress() + " a încercat să caute o carte care nu există în BD: \"" + request.getQuery() + "\" la " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
                            }
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    case CAUT_AUTOR:
                        try(var stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY)) {
                            var rs = stmt.executeQuery("SELECT * FROM admin.books WHERE lower(author) like '%" + request.getQuery() + "%'");
                            int nrCarti;
                            rs.last();
                            nrCarti = rs.getRow();
                            if(nrCarti==0)
                            {
                                out.writeObject("Nu există nicio carte cu acest autor.");
                                System.out.println("Clientul " + client.getRemoteSocketAddress() + " a încercat să caute un autor care nu există în BD: \"" + request.getQuery() + "\" la " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
                            }
                            else {
                                String[] carti = new String[nrCarti];
                                rs.beforeFirst();
                                while (rs.next()) {
                                    var numeCarte = rs.getString("name");
                                    var nrBuc = rs.getInt("stock");
                                    carti[rs.getRow() - 1] = "Cartea " + numeCarte + " are " + nrBuc + " bucăți în stoc.";
                                }
                                out.writeObject(carti);
                                System.out.println("Clientul " + client.getRemoteSocketAddress() + " a căutat autorul \"" + request.getQuery() + "\" la " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
                            }
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    case RETURNEZ:
                        try(var stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY)) {
                            var rs = stmt.executeUpdate("UPDATE admin.books set stock = stock + 1 WHERE lower(name) = '" + request.getQuery() + "'");
                            if(rs>0)
                            {
                                out.writeObject("Cartea a fost returnată cu succes. Mulțumim!");
                                System.out.println("Clientul " + client.getRemoteSocketAddress() + " a returnat cartea \"" + request.getQuery() + "\" la " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
                            }
                            else {
                                out.writeObject("Nu există nicio carte cu acest titlu.");
                                System.out.println("Clientul " + client.getRemoteSocketAddress() + " a încercat să returneze cartea \"" + request.getQuery() + "\" care nu există în BD la " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
                            }
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                        case IESIRE:
                            clienti.remove(request.getName());
                            break;
                    case NEDEFINIT:
                        out.writeObject("Comanda nu a fost recunoscută.");
                        System.out.println("Clientul " + client.getRemoteSocketAddress() + " a trimis o comandă necunoscută la " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
                        break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Clientul cu numarul "+getName()+" a închis conexiunea.");
        }
    }
}
