/**
 * This simply encapsulates the port and IP address
 */
public class SocketInfo
{
    public SocketInfo(String hostname, int port)
    {
        HOSTNAME = hostname;
        PORT = port;
        toString();
    }
    public String HOSTNAME;
    public int PORT;

    @Override
    public String toString()
    {
        return "HOSTNAME: " + HOSTNAME + " PORT: " + PORT;
    }
}
