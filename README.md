CS351
Lab 4 PublicAuction
Anna Carey - careya42
Vincent Crespin - crespinv
Anthony Galczak - Chudbrochil
Jacob Collins - jacobacollins


PublicAuction Readme:

Consult the User Guide.txt for how to run PublicAuction

PLEASE NOTE!!!
If a JavaFX "outofboundsexception" error is thrown. Please close Agent and try again.
We have spent an enormous amount of time on this bug. It is a known issue online with
Java and has absolutely nothing to do with an "outofboundsexception", it is being
thrown from a thread and then a component somewhere getting a null ref.

Explanation of design:
Per the spec, we have Agent, Bank, Auction Central and Auction House.
Each of these has a Controller (Class spanning between the GUI and the class).
Each of these has an FXML (Actual definition of GUI components).
From there, in order to make an Agent or Auction House you will need to make a new
Client which is a networking wrapper for Agent and Auction House.
In order to make an Auction Central or Bank you will need to make a new Server which
is a networking wrapper for AC and Bank.

Things we were unable to implement or finish:
After an item is sold at the Auction House we are losing the "ITEM_SOLD" msg that should be going to
the Auction Central and then to the Bank(to remove hold) and to Agent (to congratulate)
After an agent is passed on an item, the OUT_BID msg is also being lost at Auction Central.


Extras:
Images on the GUI, we didn't have time to finish this though. Look at all
    the pretty images. :( A dust bunny at least appears on the GUI.
GUIs for every class. Agent, Auction House, Auction Central, Bank
The way that standard out is captured on the GUIs is pretty clever.
Each GUI has the ability to custom connect to different IPs. This was non-trivial.
Dynamic port assignment for Clients(AH, Agent)



Message Life Cycle:
Life cycle of Registering Agent with Bank.
Agent send a message to bank saying that it would like to register an account.
Bank responds back to agent confirming that it has been registered and gives it a bank key.

Life cycle of Registering Agent with Auction Central.
Agent sends a message to Auction Central saying that it would like to register. 
Auction central responds back to Agent confirming that it has been registered and gives it a bidding key.

Life cycle of Registering Auction House with Auction central.
Auction House sends a message to Auction Central saying that it would like to register. 
Auction central responds back to Agent confirming that it has been registered and gives it a public ID.

Life cycle of placing a bid/hold.
Agent sends a message to Auction Central saying it would like to place a bid on an item from a certain Auction House.
Auction Central will send a message to that Auction House saying that Agent has placed a bid on an Item.
Auction House the sends a message back to Auction Central saying if the bid was accepted or rejected. 
If the bid was rejected then Auction Central will send back a place bid rejection to agent that his bid was unsuccessful and the message is done there.
If the bid was accepted the Auction Central will send back a bid accepted to the Back
Bank will then a place hold rejected/accepted to Auction Central.
If the hold was rejected then Auction Central sends Agent and Auction House a massage telling them that the hold was not placed so the bid was not successful. 
If the hold was accepted then Auction Central sends Agent and Auction House a massage telling them that the hold was placed so the bid was successful.

Life cycle of updating Auction Houses.
Agent sends a message to Auction Central saying that it wants an update of the Auction House and there items.
Auction central sends an updated list of Auction Houses.

Life cycle of Item Sold.
Auction House tells Auction Central that an item has been sold to an agent with a certain bidding key.
Auction central sends a message to bank telling them to remove a hold so the money can be deducted from the bank.
Auction Central also sends a message to agent letting them know that the won the item.

Life cycle of Outbid.
Auction House tells Auction Central that an item has been out bid by another agent.
Auction Central sends a message to the outbid agent bank account telling them to remove the hold since the agent was outbid.
Auction Central then sends a message to Agent letting them know that they were outbid for that item.

Life cycle of Withdraw. 
Agent sends a Message to bank saying that I want to withdrawal money from the bank.
Bank sends back a response reject/accept.
If response is accept the money was withdrawn.
If response is rejected the money was not withdrawn.

Life cycle of Unsubscribe.
If it is an Agent that is Unsubscribing from Bank and Auction Central.
Agent will send a message to Bank and auction Central telling them that I am unsubscribing from there services and closes the connections.  
If it is an Auction House that is Unsubscribing from Auction Central.
Auction House will send a message to Auction Central saying that I am unsubscribing from there services and closes the connections.



