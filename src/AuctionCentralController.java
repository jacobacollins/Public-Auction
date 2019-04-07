import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.PrintStream;

public class AuctionCentralController
{
    @FXML
    private TextArea taOutput;

    @FXML
    private Label lblAuctionHousesList, lblConnectionInfo;

    @FXML
    private TextField tfBankIP;



    @FXML
    public void initialize()
    {
        // We are allowing standard out to be printed to our text area.
        System.setOut(new PrintStream(Main.getStandardOutCapture(taOutput), true));

        // New thread for Auction Central while(true)
        Thread newThread = new Thread(new Runnable() {
            @Override
            public void run()
            {
                Server s = new Server(false, lblAuctionHousesList, lblConnectionInfo);
            }
        });

        newThread.start();
    }

    @FXML
    private void btnConnectBank()
    {
        Server.setPeerConnection(tfBankIP.getText());
    }

}
