/**
 * If you create a Message, assign it a type. Those who receive messages will respond based on the MessageType.
 */

public enum MessageType
{
    /**
     * Agent to AuctionHouse:
     *      "Hey, what items do you have?" (Establish socket communication.)
     *      "I want to place a bid on Item ____. This is my Bidding Key."
     *      "I am no longer interested in your items. Goodbye!" //closes.
     *
     * AuctionHouse to Agent:
     *      (Response) "These are my items and their minimum bids!"
     *      (Response) Accept, Reject (Item, AMOUNT)
     *      Pass, Winner (Item, AMOUNT)
     *      "Hey, I'm going out of business." //closes. CLOSE_SOCKET_REQUEST
     *
     * AuctionHouse to AuctionCentral
     *      "Please place a hold on BidderID account for AMOUNT" //PLACE_HOLD, null boolean, BidderID, AMOUNT
     *      "I'm shutting down!" CLOSE_SOCKET_REQUEST //Every time AuctionCentral gets one of these, it pings agents? to say "These houses closed!"
     *                                                  //but it doesn't have a reference... :(
     * AuctionCentral to Bank
     *      "Please place a hold on BankKey account for AMOUNT." //PLACE_HOLD, null boolean, BankKey, AMOUNT
     *
     * Bank to AuctionCentral
     *      "I placed a hold on their account." //PLACE_HOLD, true boolean, BankKey, AMOUNT
     *      "I cannot place said hold." //PLACE_HOLD, false boolean, BankKey, AMOUNT
     *
     * AuctionCentral to AuctionHouse
     *      "I placed a hold on their account." //PLACE_HOLD, true boolean, BidderID, AMOUNT
     *      "I cannot place said hold." //PLACE_HOLD, false boolean, BidderID, AMOUNT
     *
     */
    
    
    /**
     * WITHDRAW
     *      This is a test message that Agent sends to Bank to have money withdrawn from their account. (Agent sends Bank
     *      Message with AccountNum and amount.) Then the Bank responds with the same message, setting the BID_RESPONSE in Message
     *      relative to whether the money was there to withdraw or not.
     * PLACE_BID
     *      Created by an Agent and sent to AuctionHouse. The Agent sends the AuctionHouse (biddingID, amount, itemID, auctionHouseID).
     *      //todo: For now, AuctionHouse will simply record the bid.
     */
    WITHDRAW, PLACE_BID, PLACE_HOLD, UPDATE_AHS, REGISTER_AGENT, REGISTER_AH, ITEM_SOLD, UNREGISTER, GET_PORT_NUMBER, OUT_BID;
}
