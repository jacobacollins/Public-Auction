import java.io.Serializable;
import java.util.ArrayList;

/**
 * This is the object that can be sent between classes, storing information.
 * Check if a parameter is null. If it is and you're expecting to be able to use it, something has gone wrong.
 *
 */
public class Message implements Serializable
{
    //General needs
    private MessageType type;
    private BidResponse response;
    
    //for type PLACE_BID
    private double bidAmount;            // Amount the bidder wishes to bid.
    private int itemID;           //ID of the item the bidder wishes to bid on
    private int auctionHousePublicID;
    private Item item;

    //for type UPDATE_AHS
    private ArrayList<AuctionHouse> listOfAHs;

    //for type REGISTER_AGENT
    private Account account;
    private String name;
    private String bankKey;
    private String biddingKey; // Also for place_bid
    private String hostname;

    //for type REGISTER_AH
    private AuctionHouse auctionHouse;

    //for type UNREGISTER
    private boolean isAgent;
    private String clientKey; // Represents either agent bankKey or ah secretKey

    private int portNumber;
    
    //for type PLACE_HOLD
    //Everything for PLACE_BID and
    //private BidResponse response;

    public Message(MessageType t, int portNumber, String name)
    {
        type = t;
        this.name = name;
    }
    //Constructor for a UNREGISTER message
    public Message(MessageType t, boolean isAgent, String clientKey, String name)
    {
        type = t;
        this.isAgent = isAgent;
        this.setClientKey(clientKey);
        this.name = name;
    }
    
    //Constructor for a PLACE_BID message
    public Message(MessageType t, String biddingKey, double bidAmount, Item item)
    {
        type = t;
        this.biddingKey = biddingKey;
        this.bidAmount = bidAmount;
        this.item = item;
    }
    
    //Constructor for a WITHDRAW message and PLACE_HOLD
    public Message(MessageType t, String bankKey, double bidAmount)
    {
        type = t;
        this.bankKey = bankKey;
        this.setBidAmount(bidAmount);
    }

    // Constructor for a REGISTER_AGENT message (to Bank)
    public Message(MessageType t, Account account)
    {
        type = t;
        this.account = account;
    }

    //Constructor for a REGISTER_AGENT message (to AC)
    public Message(MessageType t, String name, String bankKey, String biddingKey, int portNumber, String hostname)
    {
        type = t;

        this.name = name;
        this.bankKey = bankKey;
        this.biddingKey = biddingKey;
        this.portNumber = portNumber;
        this.hostname = hostname;
    }

    //Constructor for a REGISTER_AH message (to AC)
    public Message(MessageType t, AuctionHouse auctionHouse, String hostname)
    {
        type = t;
        this.auctionHouse = auctionHouse;
        this.hostname = hostname;
    }

    //Constructor for UPDATE_AHS message
    public Message(MessageType t, ArrayList<AuctionHouse> listOfAHs)
    {
        type = t;
        this.listOfAHs = listOfAHs;
    }

    //ITEM_SOLD Constructor
    public Message(MessageType t, int item, int ahID, String bidK, Double b)
    {
        type = t;
        auctionHousePublicID = ahID;
        biddingKey = bidK;
        bidAmount = b;
        itemID = item;
    }

    //OUT_BID Constructor AND ITEM_SOLD constructor
    public Message(MessageType t, int ahID, String bKey, Double amount, Item item)
    {
        type = t;
        auctionHousePublicID = ahID;
        biddingKey = bKey;
        bidAmount = amount;
        this.item = item;
    }


    /**
     * setType()
     * @param t Type of Message.
     * For when you want to keep the same field values but need a different Message type.
     */
    public void setType(MessageType t)
    {
        type = t;
    }
    
    /**
     * getType()
     * @return Get the type of message so you can respond to it correctly
     */
    public MessageType getType()
    {
        return type;
    }
    
    public void setBidResponse(BidResponse r){response = r;}
    
    public BidResponse getBidResponse() { return response; }


    /**
     * Getters/setters for private members encapsulated in messages
     */

    public ArrayList<AuctionHouse> getListOfAHs() { return listOfAHs; }

    public void setListOfAHs(ArrayList<AuctionHouse> listOfAHs) { this.listOfAHs = listOfAHs; }

    public Account getAccount() { return account; }

    public void setAccount(Account account) { this.account = account; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getBankKey() { return bankKey; }

    public void setBankKey(String bankKey) { this.bankKey = bankKey; }

    public String getBiddingKey() { return biddingKey; }

    public void setBiddingKey(String biddingKey) { this.biddingKey = biddingKey; }

    public AuctionHouse getAuctionHouse() { return auctionHouse; }

    public void setAuctionHouse(AuctionHouse auctionHouse) { this.auctionHouse = auctionHouse; }

    public double getBidAmount()
    {
        return bidAmount;
    }

    public void setBidAmount(double bidAmount)
    {
        this.bidAmount = bidAmount;
    }

    public int getItemID()
    {
        return itemID;
    }

    public void setItemID(int itemID)
    {
        this.itemID = itemID;
    }

    public int getAuctionHousePublicID()
    {
        return auctionHousePublicID;
    }

    public void setAuctionHousePublicID(int auctionHousePublicID)
    {
        this.auctionHousePublicID = auctionHousePublicID;
    }

    public Item getItem() {
        return item;
    }

    public boolean isAgent()
    {
        return isAgent;
    }

    public void setAgent(boolean agent)
    {
        isAgent = agent;
    }

    public String getClientKey()
    {
        return clientKey;
    }

    public void setClientKey(String clientKey)
    {
        this.clientKey = clientKey;
    }

    public int getPortNumber()
    {
        return portNumber;
    }

    public void setPortNumber(int num){
        portNumber = num;
    }

    public String getHostname() { return hostname; }

}
