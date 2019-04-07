import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionHouseController
{

    @FXML
    TextArea taOutput;

    @FXML
    TextField tfAuctionCentralIP;

    @FXML
    Label lblAuctionHouseList;

    Client client;

    @FXML
    public void initialize()
    {
        // We are allowing standard out to be printed to our text area.
        System.setOut(new PrintStream(Main.getStandardOutCapture(taOutput), true));

        String name = Main.askName();

        // New thread for Auction Central while(true)
        Thread newThread = new Thread(new Runnable() {
            @Override
            public void run()
            {
                client = new Client(false, name, lblAuctionHouseList);
                //client.testTimer();
                //System.out.println(Client.getAcConnected());
            }

        });

        newThread.start();



        Thread clientListeningThread = new Thread(new Runnable() {
            @Override
            public void run()
            {
                while(true)
                {
                    // Don't delete this line, this thread gets stopped(garbage collected) without running clientListening()
                    System.out.print("");

                    // If the client isn't already listening, but is connected to the AC, start listening for msg's.
                    if(!Client.isListening() && Client.getAcConnected())
                    {
                        try { client.clientListening(); }// This will listen for messages forever
                        catch(IOException e) { System.out.println(e.getMessage()); }
                        catch(ClassNotFoundException e) { System.out.println(e.getMessage()); }
                    }
                }
            }
        });

        clientListeningThread.start();

    }


    @FXML
    private void btnConnectAC()
    {
        client.setAcHostname(tfAuctionCentralIP.getText());
    }


}
