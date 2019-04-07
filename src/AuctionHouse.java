import javafx.animation.Timeline;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Anna Carey will head the effort to implement this object.
 * <p>
 * Instances of this class are dynamically created.
 * Registers at Auction Central by providing a name and by receiving
 * --public IDs.ID,
 * --secret auction key.
 * Has a fixed number of items to sell, and no more than three at a time.
 * (each item has an auction house IDs.ID, item IDs.ID, minimum bid, and current bid.)
 * It receives bids and acknowledges them. (Agents provide their
 * Successful bid: asks AuctionCentral to place/releaes a hold on the bidder's bank account
 * for an AMOUNT equal to the bid in question
 * A bid is successful and 'wins' if it is not overtaken in 30 seconds.
 * For a successful/winning bid, the AuctionHouse requests that AuctionCentral transfer the money to the AuctionHouse.
 * <p>
 * ***
 * To use this class, you MUST set the AuctionCentral via setAuctionCentral() method.
 * ***
 */

public class AuctionHouse implements Serializable
{
    private String name;
    private int publicID;
    private String ahKey; // Requested and received from Auction Central
    private HashMap<Integer, Item> items; //Item ID as key for the item.
    private HashMap<Integer, AuctionTimer> itemTimers; //key is Item ID, timer for the winning bid.

    //private HashMap<String, Time> itemTimers;
    //private HashMap<>

    private int itemCounter = 0;

    private Item soldItem;


    /**
     * AuctionHouse()
     * Creates an AuctionHouse that has three random items for sale.
     *
     * @param name Name for this AuctionHouse
     */
    public AuctionHouse(String name)
    {
        this.name = name;
        items = new HashMap<>();
        itemTimers = new HashMap<>();
        populateItems();
    }
    
    /**
     * getItemsAsString()
     * @return A String of item toString()'s separated by '\n' characters.
     */
    public String getItemsAsString()
    {
        String output = "";
        ArrayList<Item> itemsAsList = new ArrayList<Item>(items.values());
        for(int i = 0; i < itemsAsList.size(); ++i)
        {
            output += itemsAsList.get(i).toString() + "\n";
        }
        return output;
    }

    public HashMap<Integer, Item> getItems()
    {
        return items;
    }

    /**
     * setIDs()
     *
     * Sets the publicID and auction house key of this auction house.
     *
     * @param publicID ID given to AH from the auction central
     * @param ahKey Key given to aH from the auction central
     */
    public void setIDs(int publicID, String ahKey)
    {
        this.publicID = publicID;
        this.setAhKey(ahKey);
        ArrayList<Item> itemsAsList = new ArrayList<Item>(items.values());
        for(int i = 0; i < itemsAsList.size(); ++i)
        {
            itemsAsList.get(i).setAhID(this.publicID);
        }
    }
    
    /**
     * getPublicID
     * @return get this AuctionHouse's publicID as an int.
     * Also used as the Port Number.
     */
    public int getPublicID()
    {
        return publicID;
    }
    
    /**
     * getName()
     * @return name as a String
     */
    public String getName()
    {
        return name;
    }

    /**
     * placeBid()
     * Called by an Agent to place a bid (or by a Client when a PLACE_BID Message is received)
     * @param biddingID      BIDDING_ID of the Agent who wishes to place a bid
     * @param amount         Amount the bidder wishes to bid.
     * @param itemID         ID of the item the bidder wishes to bid on
     * @param auctionHouseID the ID of this auction house (needed by Client)
     * @return true if Client should move ahead and request a hold to be placed.
     *         false if something went wrong (the Agent bid too little,) in which case the Client can send
     *         a bidResponse REJECT Message, not this AuctionHouse's ID, the item doesn't exist here)
     *         right back to the Agent.
     */
    public boolean placeBid(String biddingID, double amount, int itemID, int auctionHouseID)
    {
        //Safechecking
        if(!(auctionHouseID==publicID))
        {
            //Not the right AuctionHouse USER OUTPUT
            System.out.println(toString()+" received a placeBid request for auctionHouseID "+auctionHouseID+" which does" +
                "not match its public ID "+publicID+". Returning.");
            return false;
        }
        
        Item item = items.get(itemID);
        if (item == null)
        {
            //That item isn't for sale here USER OUTPUT
            System.out.println("Bidding ID " + biddingID + " tried to bid on " + itemID + ", which is not an item in " +
                    name + ". Returning");
            return false;
        }

        System.out.println("name: " + item.getItemName() + " minBid: " + item.getMinimumBid() + " currentBid: " + item.getCurrentBid());
        //If it's a valid bid AMOUNT
        if (amount >= item.getMinimumBid() && amount > item.getCurrentBid())
        {
            System.out.println("Valid bid");

            return true;
        }
        else
        {
            System.out.println("Not a valid bid.");
            return false;
        }
    }
    
    /**
     * processHoldResponse()
     * Called by Client when a REQUEST_HOLD Message is received.
     * @param biddingID      BIDDING_ID of the Agent who wishes to place a bid
     * @param amount         Amount the bidder wishes to bid.
     * @param itemID         ID of the item the bidder wishes to bid on
     * @param timer          AuctionTimer which has the desired event on finish for the bid item. This timer will be started and
     *                       any timer currently running for the above itemID.
     *                       THIS TIMER MUST CALL itemSold(itemID) ON THIS CLASS
     * @return  null if no bidder needs to be notified with a "pass".
     *          biddingID of the person whose bid was surpassed otherwise. Client should send a REQUEST_BID
     *          BidResponse PASS to the returned biddingID and a REQUEST_BID BidResponseMessage ACCEPT to
     *          *this* biddingID
     *
     */
    public String processHoldResponse(String biddingID, double amount, int itemID, AuctionTimer timer)
    {
        Item item = items.get(itemID);
        String prevBidWinner = item.getCurrentHighestBidderID();
        item.setCurrentBidAndBidder(amount, biddingID);
        setBidTimer(itemID, timer);
        return prevBidWinner;
    }
    


    @Override
    public String toString()
    {
        return "Name: " +  name + " Public ID: " + publicID;
    }
    
    
    /**
     * itemSold()
     * @param itemID    ID of the item stored in the timer that is called when the item is sold.
     * Called when a 'winning' item timer goes off.
     * Sets the soldItem to the item of the itemID. Does this so that Client can retrieve it by calling getSoldItem().
     * @return
     */
    public String itemSold(int itemID)
    {
        Item itemSold = items.remove(itemID);
        itemTimers.remove(itemID);
        this.soldItem = itemSold;
        return "Item "+itemSold.getItemName()+" has been sold for $"+itemSold.getCurrentBid()+"!";
    }
    
    /**
     * hasItems()
     * @return true if AuctionHouse still has items
     *         false if AuctionHouse is out of items and needs to close.
     */
    public boolean hasItems()
    {
        return !items.isEmpty();
    }
    
    /**
     * getSoldItem()
     * @return the item that was sold by the timer that just went off.
     * (set by calling itemSold.)
     */
    public Item getSoldItem()
    {
        Item sold = soldItem;
        soldItem = null;
        return sold;
    }
    
    /**
     * getAhKey()
     * @return this AuctionHouse's secret key.
     */
    public String getAhKey()
    {
        return ahKey;
    }
    
    /**
     * setAhKey()
     * @param ahKey A private key this AuctionHouse can use to be secret.
     */
    public void setAhKey(String ahKey)
    {
        this.ahKey = ahKey;
    }
    
    
    /**
     * populateItems()
     *
     * This method fills the auctionHouse with 3 random items. A given item will be initialized with a -1 for it's
     * ahID and will be initialized to an actual valid ahID after the auction house registers with auction central.
     */
    private void populateItems()
    {
        for (int i = 0; i < 3; ++i)
        {
            Item item = ItemDB.getRandomItem();
            item.setItemID(itemCounter);
            items.put(itemCounter, item);
            itemCounter++;
        }
    }
    
    /**
     * @param itemID Item whose timer is being reset
     * @param timer New AuctionTimer for the current bidder.
     */
    private void setBidTimer(int itemID, AuctionTimer timer)
    {
        if(itemTimers.containsKey(itemID))
        {
            AuctionTimer currentTimer = itemTimers.remove(itemID);
            currentTimer.stop();
        }
        itemTimers.put(itemID, timer);
        timer.playFromStart();
    }

    /**
     * itemDB inner Class
     *
     * Reads a static file to populate items into an array.
     * Has a method getRandomItem() to grab a random copy of one of these items.
     * This design is opposed to hosting a real SQL database and sending updates to it. Instead we have a file.
     */
    private static class ItemDB
    {
        private static ArrayList<Item> items;

        // Static initializer that always loads the filelist
        static
        {
            items = new ArrayList<>();

            try
            {
                InputStream inputFile = ItemDB.class.getResourceAsStream("ItemList.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputFile, "UTF-8"));
                String line;
                while ((line = reader.readLine()) != null)
                {
                    // This allows for commenting in the ItemList
                    if (!line.startsWith("//") && !line.trim().isEmpty())
                    {
                        // The ItemList is fragile, be careful editing it.
                        String[] elements = line.split(",");
                        String itemName = elements[0];
                        String imgPath = elements[1];
                        Double minimumBid = Double.valueOf(elements[2]);
                        items.add(new Item(itemName, imgPath, minimumBid));
                    }
                }
                inputFile.close();
            }
            catch (IOException e)
            {
                System.out.println(e.getMessage());
            }
        }

        /**
         * getRandomItem()
         *
         * Picks a random item from the itemList gotten from ItemList.txt and then returns a copy of it.
         * If it were to return the actual memory object then we would have comparison conflicts elsewhere in
         * the code base. Item name's don't have to be unique but their memory addresses have to be.
         *
         * @return Copy of a random item
         */
        private static Item getRandomItem()
        {
            return new Item(items.get(ThreadLocalRandom.current().nextInt(0, items.size())));
        }
    }

}
