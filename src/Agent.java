import java.io.Serializable;
import java.util.ArrayList;

public class Agent
{
    private ArrayList<AuctionHouse> auctionHouses;
    private int accountNum;
    private String bankKey, name;
    private String biddingKey;
    private double accountBalance;
    private int portNumber;


    /**
     * Agent Constructor.
     * @param name name of agent.
     */
    public Agent(String name)
    {
        auctionHouses = new ArrayList<>();
        this.name = name;
    }

    /**
     * set the account information of the agent.
     * @param account agent's acount information
     */
    public void setAccountInfo(Account account)
    {
        accountNum = account.getAccountNum();
        accountBalance = account.getAccountBalance();
        bankKey = account.getBankKey();

    }

    public void setBankKey(String bankKey)
    {
        this.bankKey = bankKey;
    }

    /**
     * Gets the agent's bank key.
     * @return bank key
     */
    public String getBankKey() { return bankKey; }

    /**
     * Sets the agent's bidding key.
     * @param newKey new Bidding key
     */
    public void setBiddingKey(String newKey)
    {
        biddingKey = newKey;
    }

    /**
     * Gets agent's bidding key.
     * @return
     */
    public String getBiddingKey()
    {
        return biddingKey;
    }

    public void setAccountNum(int accountNum)
    {
        this.accountNum = accountNum;
    }

    /**
     * Gets the agent account number
     * @return
     */
    public int getAccountNum()
    {
        return accountNum;
    }

    public void setAccountBalance(Double num)
    {
        this.accountBalance = num;
    }

    /**
     * Deducts agents account balance
     * @param num amount deducted from balance.
     */
    public void deductAccountBalance(Double num) { accountBalance -= num; }

    /**
     * gets the agent's account balance
     * @return
     */
    public double getAccountBalance() { return accountBalance; }

    /**
     * Gets agent's name
     * @return agent's name
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * List of auction house that agent can see
     * @return Array List of Auction House.
     */
    public ArrayList<AuctionHouse> getAuctionHouses() {
        return auctionHouses;
    }

    /**
     * Sets the list of auction houses
     * @param auctionHouses
     */
    public void setAuctionHouses(ArrayList<AuctionHouse> auctionHouses) {
        this.auctionHouses = auctionHouses;
    }

    /**
     * returns agent's Port Number
     * @return Port number.
     */
    public int getPortNumber() {
        return portNumber;
    }

    /**
     * Sets agents port number
     * @param portNumber agents port number
     */
    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }

}
