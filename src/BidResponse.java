/**
 * These are the kinds of responses AuctionHouse can give to Agent for placing a bid.
 * ACCEPT and REJECT also apply to Banks and holds.
 */

public enum BidResponse
{
    ACCEPT, REJECT, PASS, WIN;
}
