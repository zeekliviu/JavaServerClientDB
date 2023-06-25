import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class Server {
    public static void main(String[] args) {
        try {
        Connection conn;
        String url = "HERE WAS A CONNECTION STRING";
        Properties props = new Properties();
        props.setProperty("user", "HERE WAS AN USER");
        props.setProperty("password", "HERE WAS A PASSWORD");
        props.setProperty("javax.net.ssl.trustStore", "cwallet.sso");
        props.setProperty("javax.net.ssl.trustStoreType", "SSO");
        props.setProperty("javax.net.ssl.keyStore", "cwallet.sso");
        props.setProperty("javax.net.ssl.keyStoreType", "SSO");
        props.setProperty("oracle.net.authentication_services", "(TCPS)");
        conn = DriverManager.getConnection(url, props);
        try(var serverSocket = new ServerSocket(8989))
        {
            System.out.println("Serverul a pornit.");
            while(true)
            {
                new ServerThread(serverSocket.accept(), conn).start();
            }
        }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}