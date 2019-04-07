import javafx.application.Platform;
import javafx.scene.control.Label;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Wrapper class for AC and Bank. Listeners.
 */
public class Server
{
    private Bank bank;
    private AuctionCentral auctionCentral;
    private Label lblClientsList, lblConnectionInfo;

    private ObjectInputStream in;
    private ObjectOutputStream out;
    private static int agentPort;
    private HashMap<String, SocketInfo> agentBiddingKeyToSocketInfo;
    private HashMap<Integer, SocketInfo> ahPublicIDToSocketInfo;

    // Project CLEAN UP SERVER
    private Socket pipeConnection;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;




    private static boolean isBank;
    private static String staticAcHostname = "127.0.0.1";
    private static String staticBankHostname = "127.0.0.1";
    private static boolean bankConnectedToAC;
    private static boolean acConnectedToBank;


    /**
     * Server()
     * <p>
     * Server constructor
     *
     * @param isBank Boolean representing if this is a bank or not. True if bank, false if it's AC.
     */
    public Server(boolean isBank)
    {
        this(isBank, null, null);
    }

    public Server(boolean isBank, Label lblClientsList, Label lblConnectionInfo)
    {
        this.isBank = isBank;
        this.lblClientsList = lblClientsList;
        this.lblConnectionInfo = lblConnectionInfo;

        agentBiddingKeyToSocketInfo = new HashMap<>();
        ahPublicIDToSocketInfo = new HashMap<>();

        // If we didn't originate from the command line then spin up a thread to update the clients label
        if (lblClientsList != null) updateClientsLabel();

        try
        {
            if (isBank)
            {
                bankLaunch();
            }
            else
            {
                auctionCentralLaunch();
            }
        }
        catch (IOException e)
        {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        catch (ClassNotFoundException e)
        {
            System.out.println(e.getMessage());
        }


    }

    private void updateClientsLabel()
    {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(new Runnable()
        {
            @Override
            public void run()
            {
                Platform.runLater(() -> {
                    if (isBank)
                    {
                        lblClientsList.setText(bank.getAgentsAsString());
                    }
                    else
                    {
                        lblClientsList.setText(auctionCentral.getListOfAHsAsString());
                    }


                });
            }
        }, 0, 150, TimeUnit.MILLISECONDS);
    }

    /**
     * bankLaunch()
     * <p>
     * Launchs a bank object and opens a socket for listening for messages
     *
     * @throws IOException            Can be thrown from bad input/output in the streams
     * @throws ClassNotFoundException Can be thrown from bad cast from readObject()
     */
    private void bankLaunch() throws IOException, ClassNotFoundException
    {
        bank = new Bank();
        ServerSocket bankSocket = new ServerSocket(Main.bankPort);
        Platform.runLater(() -> lblConnectionInfo.setText(Main.returnNetworkInfo() + " Port: " + Main.bankPort));
        System.out.println("Bank online.");

        while (true)
        {
            pipeConnection = bankSocket.accept();
            outputStream = new ObjectOutputStream(pipeConnection.getOutputStream());
            inputStream = new ObjectInputStream(pipeConnection.getInputStream());
            Message incomingMessage = (Message) inputStream.readObject();
            boolean sendReturnMessage = true;

            // Performing a withdrawl for an agent
            if (incomingMessage.getType() == MessageType.WITHDRAW)
            {
                Account account = bank.getBankKeyToAccount().get(incomingMessage.getBankKey());
                System.out.println("RCV_MSG: " + incomingMessage.getType() + " - FROM: " + account.getName());
                // If we were able to deduct the bidding amount, then take it out, send a success back.
                if (account.deductAccountBalance(incomingMessage.getBidAmount()))
                {
                    incomingMessage.setBidResponse(BidResponse.ACCEPT);
                    System.out.println("Bank accepted withdrawl of " + incomingMessage.getBidAmount() + " on account:");
                }
                // If there wasn't enough money, send a rejection back.
                else
                {
                    incomingMessage.setBidResponse(BidResponse.REJECT);
                    System.out.println("Bank refused withdrawl of " + incomingMessage.getBidAmount() + " on account:");
                }
                System.out.println(account.toString());
                System.out.println("SEND_MSG: " + incomingMessage.getType() + " - TO: " + account.getName());
            }
            //When we place a bid
            else if (incomingMessage.getType() == MessageType.PLACE_BID)
            {
                Account account = bank.getBankKeyToAccount().get(incomingMessage.getBankKey());
                System.out.println("RCV_MSG: " + incomingMessage.getType() + " - FROM: Auction Central");
                incomingMessage.setType(MessageType.PLACE_HOLD);
                // If we were able to deduct the bidding amount, then take it out, send a success back.
                if (account.placeHold(incomingMessage.getBidAmount()))
                {
                    incomingMessage.setBidResponse(BidResponse.ACCEPT);
                    System.out.println("Bank has placed a hold on account for: " + incomingMessage.getBidAmount());
                }
                // If there wasn't enough money, send a rejection back.
                else
                {
                    incomingMessage.setBidResponse(BidResponse.REJECT);
                    System.out.println("Bank has refused a hold on account:");
                }
                System.out.println(account.toString());
                System.out.println("SEND_MSG: " + incomingMessage.getType() + " - TO: Auction Central");
            }
            // Initializing an agent with an account (name, account#, balance, bankkey)
            else if (incomingMessage.getType() == MessageType.REGISTER_AGENT)
            {
                System.out.println("RCV_MSG: " + incomingMessage.getType() + " - FROM: " + incomingMessage.getAccount().getName());
                bank.registerAgent(incomingMessage.getAccount());
                System.out.println("SEND_MSG: " + incomingMessage.getType() + " - TO: " + incomingMessage.getAccount().getName());
            }
            // If an agent goes offline it will unsubscribe itself from the bank.
            else if (incomingMessage.getType() == MessageType.UNREGISTER)
            {
                System.out.println("RCV_MSG: " + incomingMessage.getType() + " - FROM: " + incomingMessage.getName());
                bank.unregisterAgent(incomingMessage.getClientKey());
                System.out.println("Agent " + incomingMessage.getName() + " un-registered.");
                sendReturnMessage = false;
            }
            // removes hold from bank.
            else if (incomingMessage.getType() == MessageType.ITEM_SOLD)
            {
                Account account = bank.getBankKeyToAccount().get(incomingMessage.getBankKey());
                if(account.deductFromHold(incomingMessage.getBidAmount()))
                {
                    System.out.println("Hold successfully deducted from for $"+incomingMessage.getBidAmount()+" for biddingID "+incomingMessage.getBiddingKey());
                }
                else
                {
                    System.out.println("There was not enough money on hold bidding ID "+incomingMessage.getBiddingKey()+" to pay for this! Amount: " +
                        "$"+incomingMessage.getBidAmount());
                }

            }
            else if (incomingMessage.getType() == MessageType.OUT_BID)
            {
                System.out.println("RCV_MSG: " + incomingMessage.getType() + " - FROM: Auction Central");
                Account account = bank.getBankKeyToAccount().get(incomingMessage.getBankKey());
                if(account.releaseHold(incomingMessage.getBidAmount()))
                {
                    System.out.println("Hold successfully released for $"+incomingMessage.getBidAmount()+" for biddingID "+incomingMessage.getBiddingKey());
                }
                else
                {
                    System.out.println("There was not enough money on hold bidding ID "+incomingMessage.getBiddingKey()+" to release " +
                        "$"+incomingMessage.getBidAmount());
                }

            }

            if (sendReturnMessage) { outputStream.writeObject(incomingMessage); }

        }



    }

    /**
     * auctionCentralLaunch()
     * <p>
     * Launchs an Auction Central and opens a socket for listening to messages.
     *
     * @throws IOException            Can be thrown from bad input/output in the streams
     * @throws ClassNotFoundException Can be thrown from bad cast from readObject()
     */
    private void auctionCentralLaunch() throws IOException, ClassNotFoundException
    {
        auctionCentral = new AuctionCentral();
        ServerSocket auctionCentralSocket = new ServerSocket(Main.auctionCentralPort);

        agentPort = ThreadLocalRandom.current().nextInt(50200, 50900);
        System.out.println("Agent port: " + agentPort);

        Platform.runLater(() -> lblConnectionInfo.setText(Main.returnNetworkInfo() + " Port: " + Main.auctionCentralPort));
        System.out.println("Auction Central online.");

        while (true)
        {

            Socket otherPipeConnection = auctionCentralSocket.accept();
            ObjectOutputStream centralOut = new ObjectOutputStream(otherPipeConnection.getOutputStream());
            ObjectInputStream centralIn = new ObjectInputStream(otherPipeConnection.getInputStream());
            Message incomingMessage = (Message) centralIn.readObject();

            boolean needsReturnMessage = true;

            // Updating the list of AHs to the agent
            if (incomingMessage.getType() == MessageType.UPDATE_AHS)
            {
                incomingMessage.setListOfAHs(auctionCentral.getListOfAHs());
            }
            // Registering a new agent with AC
            else if (incomingMessage.getType() == MessageType.REGISTER_AGENT)
            {
                System.out.println("RCV_MSG: " + incomingMessage.getType() + " - FROM: " + incomingMessage.getName());
                String biddingKey = auctionCentral.registerAgent(incomingMessage.getName(), incomingMessage.getBankKey());

                incomingMessage.setBiddingKey(biddingKey);

                // Set the agent into the socket info map...
                incomingMessage.setPortNumber(agentPort);
                agentBiddingKeyToSocketInfo.put(biddingKey, new SocketInfo(incomingMessage.getHostname(), agentPort));
                agentPort++;
                System.out.println("SEND_MSG: " + incomingMessage.getType() + " - TO: " + incomingMessage.getName());
            }
            // Registering a new AH with AC
            else if (incomingMessage.getType() == MessageType.REGISTER_AH)
            {
                AuctionHouse ahToRegister = incomingMessage.getAuctionHouse();
                System.out.println("RCV_MSG: " + incomingMessage.getType() + " - FROM: " + ahToRegister.getName());
                auctionCentral.registerAuctionHouse(ahToRegister);
                ahPublicIDToSocketInfo.put(ahToRegister.getPublicID(), new SocketInfo(incomingMessage.getHostname(), ahToRegister.getPublicID()));
                System.out.println("SEND_MSG: " + incomingMessage.getType() + " - TO: " + ahToRegister.getName());
            }

            // Place_Bid came from agent
            else if (incomingMessage.getType() == MessageType.PLACE_BID && incomingMessage.getBidResponse() == null)
            {
                System.out.println("RCV_MSG: " + incomingMessage.getType() + " - FROM: bidKey-" + incomingMessage.getBiddingKey());

                // Open a new connection to the auction house that has the item to bid on.
                SocketInfo ahSocketInfo = ahPublicIDToSocketInfo.get(incomingMessage.getItem().getAhID());
                Socket auctionHouseSocket = new Socket(ahSocketInfo.HOSTNAME, ahSocketInfo.PORT);

                out = new ObjectOutputStream(auctionHouseSocket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(auctionHouseSocket.getInputStream());

                out.writeObject(incomingMessage);
                System.out.println("SEND_MSG: " + incomingMessage.getType() + " - TO: AH-ID: " + incomingMessage.getItem().getAhID());

                incomingMessage = (Message) in.readObject(); // Message in from AH.
                System.out.println("RCV_MSG: " + incomingMessage.getType() + " - FROM: AH-ID: " + incomingMessage.getItem().getAhID());

                if (incomingMessage.getBidResponse() == BidResponse.REJECT)
                {
                    centralOut.writeObject(incomingMessage);
                    System.out.println("Auction House " + incomingMessage.getItem().getAhID() + " says this isn't a valid bid.");

                    // Open new connection to send the bid rejection to the agent.
                    SocketInfo agentSocketInfo = agentBiddingKeyToSocketInfo.get(incomingMessage.getBiddingKey());
                    Socket agentSocket = new Socket(agentSocketInfo.HOSTNAME, agentSocketInfo.PORT);
                    out = new ObjectOutputStream(agentSocket.getOutputStream());
                    in = new ObjectInputStream(agentSocket.getInputStream());

                    out.writeObject(incomingMessage);

                }
                else if (incomingMessage.getBidResponse() == BidResponse.ACCEPT)
                {
                    Socket bankSocket = new Socket(staticBankHostname, Main.bankPort);
                    ObjectOutputStream outToBank = new ObjectOutputStream(bankSocket.getOutputStream());
                    ObjectInputStream inFromBank = new ObjectInputStream(bankSocket.getInputStream());

                    incomingMessage.setBankKey(auctionCentral.getBiddingKeyToBankKey().get(incomingMessage.getBiddingKey()));
                    outToBank.writeObject(incomingMessage);
                    System.out.println("SEND_MSG: " + incomingMessage.getType() + " - TO: Bank");


                    Message bankResponse = (Message) inFromBank.readObject();
                    bankResponse.setBiddingKey(auctionCentral.getBankKeyToBiddingKey().get(incomingMessage.getBankKey()));

                    // We get a response back from the bank on whether or not the agent had the money.
                    System.out.println("RCV_MSG: " + bankResponse.getType() + " - FROM: Bank");
                    System.out.println("SEND_MSG: " + bankResponse.getType() + " - TO: AH-ID: " + incomingMessage.getItem().getAhID());
                    System.out.println("SEND_MSG " + bankResponse.getType() + " - TO: bidKey-" + incomingMessage.getBiddingKey());
    
                    ////////
                    ahSocketInfo = ahPublicIDToSocketInfo.get(incomingMessage.getItem().getAhID());
                    auctionHouseSocket = new Socket(ahSocketInfo.HOSTNAME, ahSocketInfo.PORT);
    
                    out = new ObjectOutputStream(auctionHouseSocket.getOutputStream());
                    out.flush();
                    in = new ObjectInputStream(auctionHouseSocket.getInputStream());
                    ////////

                    out.writeObject(bankResponse);
                    centralOut.writeObject(bankResponse);
                    //centralOut.writeObject(incomingMessage);
                }

                needsReturnMessage = false;


            }
            // If an agent or AH goes down it will unsubscribe from the auction central
            else if (incomingMessage.getType() == MessageType.UNREGISTER)
            {
                System.out.println("RCV_MSG: " + incomingMessage.getType() + " - FROM: " + incomingMessage.getName());
                if (!incomingMessage.isAgent())
                {
                    auctionCentral.unregisterAuctionHouse(incomingMessage.getClientKey());
                    System.out.println("Auction House " + incomingMessage.getName() + " un-registered.");
                }
                else
                {
                    System.out.println("Agent " + incomingMessage.getName() + " un-registered.");
                }

                needsReturnMessage = false;
            }
            // sends a message of ITEM_SOLD to bank and agent.
            else if (incomingMessage.getType() == MessageType.ITEM_SOLD)
            {
                System.out.println("RCV_MSG: " + incomingMessage.getType() + " - FROM: " + incomingMessage.getName());
                Socket bankSocket = new Socket(staticBankHostname, Main.bankPort);

                out = new ObjectOutputStream(bankSocket.getOutputStream());
                in = new ObjectInputStream(bankSocket.getInputStream());
                
                String bankKey = auctionCentral.getBiddingKeyToBankKey().get(incomingMessage.getBiddingKey());
                incomingMessage.setBankKey(bankKey);
                
                //get Agent socket
                SocketInfo agentSocketInfo = agentBiddingKeyToSocketInfo.get(incomingMessage.getBiddingKey());
                Socket agentSocket = new Socket(agentSocketInfo.HOSTNAME, agentSocketInfo.PORT);
                ObjectOutputStream agentOut = new ObjectOutputStream(agentSocket.getOutputStream());
    
                // Sending a message of type Item_Sold.
                System.out.println("SEND_MSG: " + incomingMessage.getType() + " - TO: bidID "+ incomingMessage.getBiddingKey());
                agentOut.writeObject(incomingMessage);
                out.writeObject(incomingMessage);
                
                needsReturnMessage = false;
            }
            else if (incomingMessage.getType() == MessageType.OUT_BID)
            {
                System.out.println("SEND_MSG: " + incomingMessage.getType() + " - TO: bidID "+ incomingMessage.getBiddingKey());
                Socket bankSocket = new Socket(staticBankHostname, Main.bankPort);

                out = new ObjectOutputStream(bankSocket.getOutputStream());
                in = new ObjectInputStream(bankSocket.getInputStream());

                // ****** I changed this code to add the bankKey to the message and forward it. --Anna
                String bankKey = auctionCentral.getBiddingKeyToBankKey().get(incomingMessage.getBiddingKey());
                incomingMessage.setBankKey(bankKey);


                out.writeObject(incomingMessage);
                System.out.println("SEND_MSG: " + incomingMessage.getType() + " - TO: Bank");


                // ToDO make ac talk to agent.

                // Sending a message of type OUT_BID.
                
                //get Agent socket
                SocketInfo agentSocketInfo = agentBiddingKeyToSocketInfo.get(incomingMessage.getBiddingKey());
                Socket agentSocket = new Socket(agentSocketInfo.HOSTNAME, agentSocketInfo.PORT);
                ObjectOutputStream agentOut = new ObjectOutputStream(agentSocket.getOutputStream());

                // Sending a message of type OUT_BID.
                agentOut.writeObject(incomingMessage);
                out.writeObject(incomingMessage);
                needsReturnMessage = false;
            }

            if (needsReturnMessage)
            {
                centralOut.writeObject(incomingMessage);
            }


        }

    }

//    private int getPortNumber()
//    {
//        ++portNumber;
//        return portNumber;
//    }

    /**
     * setPeerConnection()
     * <p>
     * Connects a bank to an AC or an AC to a bank.
     *
     * @param peerHostname Hostname we are connecting to.
     */
    public static void setPeerConnection(String peerHostname)
    {
        if (isBank)
        {
            staticAcHostname = peerHostname;
            bankConnectedToAC = true;
            System.out.println("Initializing Auction Central to: " + staticAcHostname + ":" + Main.auctionCentralPort);
        }
        else
        {
            staticBankHostname = peerHostname;
            acConnectedToBank = true;
            System.out.println("Initializing Bank connection to: " + staticBankHostname + ":" + Main.bankPort);
        }
    }

    /**
     * main()
     * <p>
     * Strictly for spinning up Servers on the command line. Mostly used for debugging.
     */
    public static void main(String[] args)
    {
        if (args[0].equals("Bank"))
        {
            Server s = new Server(true);
        }
        else if (args[0].equals("AC"))
        {
            Server s = new Server(false);
        }
    }


}