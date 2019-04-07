import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Wrapper class for AuctionHouse and Agents. Sends and receives messages.
 */
public class Client
{
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Socket bankSocket;
    private Socket auctionCentralSocket;
    private TextArea taAgentOutput;
    private Label lblAuctionHouseList;
    private ServerSocket listeningSocket;
    private Socket pipeConnection;
    private Item soldItem;

    // These need to be static so that we can eventually terminate our connection to the AC and Bank
    private static boolean isListening;
    private static Agent agent;
    private static AuctionHouse auctionHouse;
    private static boolean isAgent;
    private static boolean acConnected;
    private static boolean bankConnected;
    private static String staticBankHostname = "127.0.0.1";
    private static String staticACHostname = "127.0.0.1";


    /**
     * Client()
     * Constructor useful for running on command line
     *
     * @param isAgent Boolean value representing whether or not we're making an Agent or AH. true makes Agent, false AH
     * @param name    Name of the object (agent or AH) we are creating.
     */
    public Client(boolean isAgent, String name)
    {
        this(isAgent, name, null);
    }

    /**
     * Client()
     * Regular constructor for Client that gets called and updates the GUI via a text area
     *
     * @param isAgent          Boolean value representing whether or not we're making an Agent or AH. true makes Agent, false AH
     * @param name             Name of the object (agent or AH) we are creating.
     * @param agentOrAHControl This Control represents either an auction house label that will get updated with
     *                         a list of items or an agent text area that gets status updates
     */
    public Client(boolean isAgent, String name, Control agentOrAHControl)
    {
        Client.isAgent = isAgent;

        bankConnected = false;
        acConnected = false;

        //Client.isListening = false;

        if (name == null)
        {
            name = "NONAME CLIENT";
        }

        if (isAgent)
        {
            taAgentOutput = (TextArea) agentOrAHControl;
            agent = new Agent(name);
            if (taAgentOutput != null)
            {
                taAgentOutput.appendText("Hello, " + agent.getName() + ".\n");
            }
        }
        else
        {
            lblAuctionHouseList = (Label) agentOrAHControl;
            auctionHouse = new AuctionHouse(name);
            System.out.println("Welcome, " + auctionHouse.getName() + ".\n");
            updateAuctionHouseListLabel();
        }

        // Console only supports localhost connections for now.
        if (taAgentOutput == null && lblAuctionHouseList == null)
        {
            connectLocalhost();
        }

    }

    /**
     * updateAuctionHouseListLabel()
     * <p>
     * Spins up a thread to update the label on the AuctionHouse GUI. This Label corresponds to the list of items
     * that is currently in the Auction House.
     */
    private void updateAuctionHouseListLabel()
    {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(new Runnable()
        {
            @Override
            public void run()
            {
                // Displaying the auction house' items in a label
                Platform.runLater(() -> {
                    lblAuctionHouseList.setText(auctionHouse.getItemsAsString());
                });
            }
        }, 0, 150, TimeUnit.MILLISECONDS);
    }

    /**
     * registerAgentWithBank()
     * <p>
     * Sends a message from this client(agent) to bank to register itself as an agent.
     * This initializes the agent's account(including bankkey, accountnum and startingbalance)
     *
     * @throws IOException            Can be thrown from bad input/output in the streams
     * @throws ClassNotFoundException Can be thrown from bad cast from readObject()
     */
    private void registerAgentWithBank() throws IOException, ClassNotFoundException
    {
        out = new ObjectOutputStream(bankSocket.getOutputStream());
        in = new ObjectInputStream(bankSocket.getInputStream());
        out.writeObject(new Message(MessageType.REGISTER_AGENT, new Account(agent.getName())));
        Message response = (Message) in.readObject();
        agent.setAccountInfo(response.getAccount());

        if (taAgentOutput != null)
        {
            taAgentOutput.appendText("Starting Balance: " + agent.getAccountBalance() + "\n");
            taAgentOutput.appendText("Account Number: " + agent.getAccountNum() + "\n");
            taAgentOutput.appendText("Bank Key: " + agent.getBankKey() + "\n");
        }
        bankConnected = true;

    }

    /**
     * registerAgentWithAC()
     * <p>
     * Sends a message from this client(agent) to auction central to register itself as an agent.
     * This is how an agent gets its bidding key.
     *
     * @throws IOException            Can be thrown from bad input/output in the streams
     * @throws ClassNotFoundException Can be thrown from bad cast from readObject()
     */
    private void registerAgentWithAC() throws IOException, ClassNotFoundException
    {
        out = new ObjectOutputStream(auctionCentralSocket.getOutputStream());
        in = new ObjectInputStream(auctionCentralSocket.getInputStream());

        InetAddress ipInfo = InetAddress.getLocalHost();
        String hostname = ipInfo.getHostName();

        out.writeObject(new Message(MessageType.REGISTER_AGENT, agent.getName(), agent.getBankKey(), "", 0, hostname));
        Message response = (Message) in.readObject();
        agent.setPortNumber(response.getPortNumber());
        agent.setBiddingKey(response.getBiddingKey());
        taAgentOutput.appendText("Bidding Key: " + response.getBiddingKey() + "\n");
        acConnected = true;
    }

    /**
     * registerAHWithAC()
     * <p>
     * Sends a message from this client(auction house) to auction central to register itself as an AH.
     * This initializes everything about the auction house.
     *
     * @throws IOException            Can be thrown from bad input/output in the streams
     * @throws ClassNotFoundException Can be thrown from bad cast from readObject()
     */
    private void registerAHWithAC() throws IOException, ClassNotFoundException
    {
        out = new ObjectOutputStream(auctionCentralSocket.getOutputStream());
        in = new ObjectInputStream(auctionCentralSocket.getInputStream());
        InetAddress ipInfo = InetAddress.getLocalHost();
        String hostname = ipInfo.getHostName();
        out.writeObject(new Message(MessageType.REGISTER_AH, auctionHouse, hostname));
        Message incomingMessage = (Message) in.readObject();
        auctionHouse = incomingMessage.getAuctionHouse();
        acConnected = true;
    }

    /**
     * getAgent()
     *
     * @return The agent that is held within this client
     */
    public Agent getAgent()
    {
        return agent;
    }


    /**
     * placeAHBid()
     *
     * This gets called when an agent clicks the place bid button. This will send a message from
     * Agent->AC->AH(Check Item/BidAmt)->AC->Bank($ check)->AC to Agent and AH for final response.
     *
     * If the resulting PLACE_HOLD is a success then the Agent successfully placed a bid.
     * Otherwise
     *
     * @param bidAmount
     * @param biddingKey
     * @param item
     */
    public void placeAHBid(double bidAmount, String biddingKey, Item item)
    {
        try
        {
            auctionCentralSocket = new Socket(staticACHostname, Main.auctionCentralPort);

            out = new ObjectOutputStream(auctionCentralSocket.getOutputStream());
            in = new ObjectInputStream(auctionCentralSocket.getInputStream());


            System.out.println("writing to ac...");
            out.writeObject(new Message(MessageType.PLACE_BID, biddingKey, bidAmount, item));

            Message response = (Message) in.readObject();
            System.out.println("received message back from ac");


            if(response.getBidResponse() == BidResponse.ACCEPT)
            {
                taAgentOutput.appendText("Congratulations! You successfully bid: " + response.getBidAmount() + " on " + response.getItem().getItemName());
            }
            else
            {
                taAgentOutput.appendText("Your bid of: " + response.getBidAmount() + " was refused.");
            }

            //System.out.println("INSIDE PLACEAHBID " + response.getBidAmount() + " " + response.getName());



            //response.getItem().setCurrentBidAndBidder(response.getBidAmount(), response.getName());

        }
        catch (IOException e)
        {

            System.out.println(e.getMessage());
            System.out.println(e.getLocalizedMessage());
            e.printStackTrace();


        }
        catch (ClassNotFoundException e)
        {
            System.out.println(e.getMessage());
        }

    }

    /**
     * withdraw()
     * <p>
     * Sends a message to the bank to withdraw a fixed amount from their account. This wasn't a requirement
     * of the project, but serves as a great proof of concept and theoretical feature.
     *
     * @param withdrawl The amount the agent is trying to withdraw
     * @param agent     The agent that wants to withdraw money.
     */
    public void withdraw(double withdrawl, Agent agent)
    {
        try
        {
            if (bankConnected)
            {
                bankSocket = new Socket(staticBankHostname, Main.bankPort);
                out = new ObjectOutputStream(bankSocket.getOutputStream());
                in = new ObjectInputStream(bankSocket.getInputStream());

                // Sending a message of type Withdraw
                out.writeObject(new Message(MessageType.WITHDRAW, agent.getBankKey(), withdrawl));
                Message response = (Message) in.readObject();

                if (response.getBidResponse() == BidResponse.ACCEPT)
                {
                    agent.deductAccountBalance(response.getBidAmount());
                    if (taAgentOutput != null)
                    {
                        taAgentOutput.appendText("Withdraw accepted. New balance: " + agent.getAccountBalance() + "\n");
                    }
                }
                else
                {
                    if (taAgentOutput != null)
                    {
                        taAgentOutput.appendText("You don't have enough funds to withdraw " + response.getBidAmount() + "\n");
                    }
                }
            }

        }
        catch (IOException e)
        {
            System.out.println(e.getMessage());
        }
        catch (ClassNotFoundException e)
        {
            System.out.println(e.getMessage());
        }
    }


    /**
     * updateListOfAHs()
     * <p>
     * Opens a socket to the AC and updates the agent's list of auction house's
     */
    public void updateListOfAHs()
    {
        try
        {
            if (acConnected)
            {
                auctionCentralSocket = new Socket(staticACHostname, Main.auctionCentralPort);
                out = new ObjectOutputStream(auctionCentralSocket.getOutputStream());
                in = new ObjectInputStream(auctionCentralSocket.getInputStream());
                out.writeObject(new Message(MessageType.UPDATE_AHS, new ArrayList<AuctionHouse>()));

                Message response = (Message) in.readObject();

                if (response.getType() == MessageType.UPDATE_AHS)
                {
                    agent.setAuctionHouses(response.getListOfAHs());
                }
                else
                {
                    System.out.print("Whoops, received a message other than update AHs");
                }
            }

        }
        catch (IOException e)
        {
            System.out.println(e.getMessage());
        }
        catch (ClassNotFoundException e)
        {
            System.out.println(e.getMessage());
        }
    }

    /**
     * setBankHostname()
     * <p>
     * Sets the hostname for the banksocket and then connects to the bank. If this Client is an agent, the agent
     * will register itself with the bank.
     *
     * @param bankHostname The hostname given to us to connect to
     */
    public void setBankHostname(String bankHostname)
    {
        staticBankHostname = bankHostname;

        try
        {
            // Only the agent needs a connection to the bank.
            if (isAgent && !bankConnected)
            {
                bankSocket = new Socket(bankHostname, Main.bankPort);
                registerAgentWithBank();
                taAgentOutput.appendText("Connecting and registering with bank at: " + bankHostname + ":" + Main.bankPort + "\n");
            }

        }
        catch (IOException e)
        {
            System.out.println(e.getMessage());
        }
        catch (ClassNotFoundException e)
        {
            System.out.println(e.getMessage());
        }
    }

    /**
     * setAcHostname()
     * <p>
     * Sets the hostname for the auctionCentralSocket and then connect to the Auction Central.
     * Then the AH or Agent registers with the Auction Central.
     *
     * @param acHostname
     */
    public void setAcHostname(String acHostname)
    {
        staticACHostname = acHostname;

        try
        {
            if (!acConnected)
            {

                if (isAgent && bankConnected)
                {
                    auctionCentralSocket = new Socket(acHostname, Main.auctionCentralPort);
                    registerAgentWithAC();
                    taAgentOutput.appendText("Connecting and registering with AC at: " + acHostname + ":" + Main.auctionCentralPort + "\n");
                }
                else if (isAgent && !bankConnected)
                {
                    taAgentOutput.appendText("Cannot connect to AC. Register with bank first to get your bank key.\n");
                }
                else
                {
                    auctionCentralSocket = new Socket(acHostname, Main.auctionCentralPort);
                    registerAHWithAC();
                    System.out.println("Connecting and registering with AC at: " + acHostname + ":" + Main.auctionCentralPort + "\n");
                }
            }


        }
        catch (IOException e)
        {
            System.out.println(e.getMessage());
        }
        catch (ClassNotFoundException e)
        {
            System.out.println(e.getMessage());
        }
    }

    /**
     * getAcConnected()
     *
     * @return Boolean representing whether this client has connected with the AC already.
     */
    public static boolean getAcConnected()
    {
        return acConnected;
    }

    /**
     * connectLocalhost()
     * <p>
     * Sets both of the bank and ac hostname's to localhost. This is very useful for working on the command line and
     * in most situations where all the nodes are on the same machine.
     */
    public void connectLocalhost()
    {
        setBankHostname("127.0.0.1");
        setAcHostname("127.0.0.1");
    }
    //Sends mssage to ac from ah to say item was sold.
//    public void itemSold(Item item, Double totalBid, Agent agent) {
//        try {
//            auctionCentralSocket = new Socket("127.0.0.1", 5555);
//            out = new ObjectOutputStream(auctionCentralSocket.getOutputStream());
//            in = new ObjectInputStream(auctionCentralSocket.getInputStream());
//            out.writeObject(new Message(MessageType.ITEM_SOLD, item.getItemID(), auctionHouse, agent.getBiddingKey(), totalBid));
//
//            Message response = (Message) in.readObject();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            e.getLocalizedMessage();
//            e.getMessage();
//        }
//    }

    public void clientListening() throws IOException, ClassNotFoundException
    {
        isListening = true;
        if (isAgent)
        {
            System.out.println("Agent thinks the port is " + agent.getPortNumber());
            listeningSocket = new ServerSocket(agent.getPortNumber());
            taAgentOutput.appendText("Agent listening for msg's on port " + getAgent().getPortNumber() + "\n");
        }
        else
        {
            System.out.println("AH thinks the port is: " + auctionHouse.getPublicID());
            listeningSocket = new ServerSocket(auctionHouse.getPublicID());
            System.out.println(auctionHouse.getName() + " listening for msg's on port " + auctionHouse.getPublicID());
        }


        while(true)
        {
            pipeConnection = listeningSocket.accept();
            out = new ObjectOutputStream(pipeConnection.getOutputStream());
            in = new ObjectInputStream(pipeConnection.getInputStream());
            Message incomingMessage = (Message) in.readObject();
            // Agent listening
            if(isAgent)
            {
                if (incomingMessage.getType() == MessageType.PLACE_HOLD)
                {
                    System.out.println("RCV_MSG: " + incomingMessage.getType() + " - FROM: Auction Central");
                    if (incomingMessage.getBidResponse() == BidResponse.REJECT)
                    {
                        System.out.println("You didn't have $"+incomingMessage.getBidAmount()+" for "+incomingMessage.getItem().getItemName());
                    }
                    else if (incomingMessage.getBidResponse() == BidResponse.ACCEPT)
                    {
                        System.out.println("Good job you blew $"+incomingMessage.getBidAmount()+"on "+incomingMessage.getItem().getItemName());
                    }
                }
                else if (incomingMessage.getType() == MessageType.ITEM_SOLD)
                {
                    System.out.println("RCV_MSG: " + incomingMessage.getType() + " - FROM: " + incomingMessage.getItem().getCurrentHighestBidderID());
                    System.out.println("You won "+incomingMessage.getItem().getItemName()+" for "+incomingMessage.getBidAmount());
                }
                else if (incomingMessage.getType() == MessageType.PLACE_BID && incomingMessage.getBidResponse() == BidResponse.REJECT)
                {
                    System.out.println("RCV_MSG: " + incomingMessage.getType() + " - FROM: Auction Central");
                    System.out.println("Your bid on "+incomingMessage.getItem().getItemName()+" was rejected from AH:" + incomingMessage.getItem().getAhID());
                }
            }
            // Auction House listening
            else
            {
                if (incomingMessage.getType() == MessageType.PLACE_BID)
                {
                    System.out.println("RCV_MSG: " + incomingMessage.getType() + " - FROM: Auction Central");
                    // Placing a bid for your item at the auction house.
                    if (auctionHouse.placeBid(incomingMessage.getBiddingKey(), incomingMessage.getBidAmount(),
                            incomingMessage.getItem().getItemID(), incomingMessage.getItem().getAhID()))
                    {
                        incomingMessage.setBidResponse(BidResponse.ACCEPT);
                    }
                    else
                    {
                        incomingMessage.setBidResponse(BidResponse.REJECT);
                    }
                    out.writeObject(incomingMessage);
                }
                else if (incomingMessage.getType() == MessageType.PLACE_HOLD)
                {
                    System.out.println("RCV_MSG: " + incomingMessage.getType() + " - FROM: Auction Central");
                    if (incomingMessage.getBidResponse() == BidResponse.ACCEPT)
                    {
                        acceptBid(incomingMessage);

                        //All other code moved into above method.

                    }
                    else if (incomingMessage.getBidResponse() == BidResponse.REJECT)
                    {
                        System.out.println("Your bid was rejected due to lack of funds.");

                    }
                }
            }
        }



    }
    
    /**
     * acceptBid()
     * @param incomingMessage Message of type PLACE_HOLD with BidResponse.ACCEPT.
     * @throws IOException if the out stream throws an exception when writing to it.
     *
     * **This method was encapsulated to allow for testing.
     */
    private void acceptBid(Message incomingMessage) throws IOException
    {
        //Make a new timer
        AuctionTimer timer = new AuctionTimer(incomingMessage.getItem());
        System.out.println("Making a new timer for a bidded on item!"); //***
        timer.setOnFinished(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent arg0)
            {
                String itemSoldReport = auctionHouse.itemSold(incomingMessage.getItem().getItemID());
                System.out.println(itemSoldReport);
                setSoldItem(auctionHouse.getSoldItem());
                //System.out.println("Timer for "+incomingMessage.getItem().toString()+" just went off!");
                
                boolean ahHasItems = auctionHouse.hasItems();
                if(!ahHasItems)
                {
                    try { unsubscribe();}
                    catch(Exception e)
                    {
                        System.out.println(e.getMessage());
                        System.out.println(e.getLocalizedMessage());
                        e.printStackTrace();
                    }
                }
                
                notifyWinner(soldItem);
            }
        });
        
        
        double prevBidAmount = incomingMessage.getItem().getCurrentBid();
        //System.out.println("THE BID AMOUNT INSIDE ACCEPT BID IS: " + incomingMessage.getBidAmount());
        String prevBidder = auctionHouse.processHoldResponse(incomingMessage.getBiddingKey(), incomingMessage.getBidAmount(),
            incomingMessage.getItem().getItemID(), timer);
        if(prevBidder != null)
        {
            System.out.println("Sending OUT_BID message to bidID "+ prevBidder);
            sendOutBidMessage(prevBidAmount, prevBidder, incomingMessage.getItem());
        }
        
        //Comment out this line if you want to run testTimer()
        out.writeObject(incomingMessage);
    }
    
    /**
     * notifyWinner()
     * @param item that has been sold! The bidding ID and amount is stored in it.
     */
    private void notifyWinner(Item item)
    {
        try
        {
            System.out.println("Bidding ID"+ item.getCurrentHighestBidderID()+ " just won "+item.getItemName()+" for $"+item.getCurrentBid()+"!");
            Message winnerMessage = new Message(MessageType.ITEM_SOLD, auctionHouse.getPublicID(), item.getCurrentHighestBidderID(),
                    item.getCurrentBid(), item);
    
            out = new ObjectOutputStream(pipeConnection.getOutputStream());
            
            try{Thread.sleep(2);}
            catch(Exception e) {}
            out.writeObject(winnerMessage);
        }
        catch(IOException e) { System.out.println(e.getMessage()); }

    }
    
    /**
     * sendOutBidMessage()
     * @param prevBidAmount The amount of the bid that is now null--needed by the bank to release the right amount from the hold.
     * @param prevBidder The biddingID of the bidder who should be notified that their bid has been passed.
     * @param item The item on which the bid was outbid.
     * @throws IOException Thrown from writing to the out port. Caught by the larger body method.
     */
    private void sendOutBidMessage(double prevBidAmount, String prevBidder, Item item) throws IOException
    {
        Message outbidMessage = new Message(MessageType.OUT_BID, auctionHouse.getPublicID(), prevBidder, prevBidAmount, item);
    
        out = new ObjectOutputStream(pipeConnection.getOutputStream());
        out.writeObject(outbidMessage);
    }
    
    /**
     * testTimer()
     * Run to test whether the timers work or not. Creates a message that is phoey that causes Client to act as though AH just received a
     * valid bid for its item with ID 0. Sets the timer. It should go off and possibly print a message.
     *
     * NOTE: You will need to comment out any lines that send outgoing messages in acceptBid() for this test method to work.
     *
     * For testing only.
     */
    public void testTimer()
    {
        Item item = auctionHouse.getItems().get(0); //the first item
        Message message = new Message(MessageType.PLACE_HOLD, "fooBiddingKey", 10000, item);
        message.setBidResponse(BidResponse.ACCEPT);
        try{acceptBid(message);}
        catch(Exception e)
        {
            System.out.println(e.getMessage());
            System.out.println(e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    /**
     * unsubscribe()
     * <p>
     * When the GUI is closed for an Agent or an Auction House, this method is called in it's onClose()
     * From here we then send unsubscribe messages to AC and Bank, depending on what kind of Client this is.
     * This allows us to "graciously close" our connections.
     *
     * @throws IOException If we open a bad socket, IOException is thrown.
     */
    public static void unsubscribe() throws IOException
    {
        String name = "";
        String clientKey = "";
        ObjectOutputStream out;

        // Unsubscribing the agent from the bank.
        if (isAgent && bankConnected)
        {
            name = agent.getName();
            clientKey = agent.getBankKey();
            Socket staticBankSocket = new Socket(staticBankHostname, Main.bankPort);
            out = new ObjectOutputStream(staticBankSocket.getOutputStream());
            out.writeObject(new Message(MessageType.UNREGISTER, isAgent, clientKey, name));
        }
        else if(!isAgent)
        {
            name = auctionHouse.getName();
            clientKey = auctionHouse.getAhKey();
        }

        // Sending the message for either agent or ah, it's generalized for each
        if (acConnected)
        {
            Socket staticAcSocket = new Socket(staticACHostname, Main.auctionCentralPort);
            out = new ObjectOutputStream(staticAcSocket.getOutputStream());
            out.writeObject(new Message(MessageType.UNREGISTER, isAgent, clientKey, name));
        }
    }

    /**
     * main()
     * <p>
     * Strictly for spinning up Clients on the command line. Mostly used for debugging.
     *
     * @param args First arg decides if you want an AH or Agent, second is the name
     */
    public static void main(String[] args)
    {
        if (args[0].equals("AH") && !args[1].equals(null))
        {
            Client client = new Client(false, args[1]);
        }
        else if (args[0].equals("Agent") && !args[1].equals(null))
        {
            Client client = new Client(true, args[1]);
        }
    }

    public Item getSoldItem()
    {
        return soldItem;
    }

    public void setSoldItem(Item soldItem)
    {
        this.soldItem = soldItem;
    }

    public static boolean isListening()
    {
        return isListening;
    }

}
