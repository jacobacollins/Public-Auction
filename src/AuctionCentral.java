import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

public class AuctionCentral
{

    private HashMap<String, String> biddingKeyToBankKey;
    private HashMap<String, String> bankKeyToBiddingKey;
    private HashMap<String, Integer> agentNameToPort;

    private ArrayList<AuctionHouse> listOfAHs;
    private static int auctionHouseID = 50100; //This will become the port# for the AH.




    /**
     * auctionCentral()
     *
     * Standard constructor for auction central.
     */
    public AuctionCentral()
    {
        biddingKeyToBankKey = new HashMap<>();
        bankKeyToBiddingKey = new HashMap<>();
        agentNameToPort = new HashMap<>();
        listOfAHs = new ArrayList<>();
    }

    /**
     * registerAgent()
     *
     * Registers an agent with the auction central. This gives the agent back a bidding key.
     * The intention is that the agent has already registered with the bank as it needs to give the
     * auction central it's bank key to get back a bidding key.
     *
     * @param agentName Name of the agent being registered with auction central.
     * @param bankKey Bank key of th agent being registered. Gets mapped to a bidding key.
     * @return The bidding key assigned to the agent.
     */
    public String registerAgent(String agentName, String bankKey)
    {
        System.out.println("Agent " + agentName + " registered.");
        String biddingKey = Bank.getKey(agentName + bankKey);
        biddingKeyToBankKey.put(biddingKey, bankKey);
        bankKeyToBiddingKey.put(bankKey, biddingKey);
        return biddingKey;
    }

    /**
     * registerAuctionHouse()
     *
     * Registers an auction house with auction central. Auction central holds a list of AHs that an agent will
     * want to retrieve. This is also where the auction house receives it's public id and auction house key
     *
     * @param auctionHouse The auction house that needs to register with the auction central.
     */
    public void registerAuctionHouse(AuctionHouse auctionHouse)
    {
        String auctionHouseKey = Bank.getKey(auctionHouse.getName());
        auctionHouse.setIDs(auctionHouseID, auctionHouseKey);
        listOfAHs.add(auctionHouse);
        System.out.println("Auction House " + auctionHouse.getName() + " registered.");
        auctionHouseID++;
    }

    /**
     * unregisterAuctionHouse()
     *
     * When an auction house wants to terminate, it will unsubscribe from the list of auction houses.
     * This change will be reverberated to any agents that are subscribed to AC.
     *
     * @param ahKey The unique auction house key that the AH got when it registered with AC the first time.
     */
    public void unregisterAuctionHouse(String ahKey)
    {
        for(int i = 0; i < listOfAHs.size(); ++i)
        {
            if(ahKey.equals(listOfAHs.get(i).getAhKey()))
            {
                listOfAHs.remove(i);
                break;
            }
        }
    }

    /**
     * getListOfAHs()
     *
     * Retrieves the list of auction houses registered with auction central.
     *
     * @return List of auction houses.
     */
    public ArrayList<AuctionHouse> getListOfAHs() { return listOfAHs; }

    public String getListOfAHsAsString()
    {
        String output = "";
        if(!listOfAHs.isEmpty())
        {
            for(int i = 0; i < listOfAHs.size(); ++i)
            {
                output += listOfAHs.get(i).toString() + "\n";
            }

        }
        else
        {
            output = "No Auction House's registered.";
        }
        return output;

    }

    public HashMap<String, Integer> getAgentNameToPort() {
        return agentNameToPort;
    }

    public void setAgentNameToPort(String agentName, int portNumber) {
        agentNameToPort.put(agentName, portNumber);
    }

    public HashMap<String, String> getBiddingKeyToBankKey() {
        return biddingKeyToBankKey;
    }

    public HashMap<String, String> getBankKeyToBiddingKey() {
        return bankKeyToBiddingKey;
    }

}

