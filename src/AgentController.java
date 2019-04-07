import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AgentController
{
    @FXML
    private Label lblBalance;

    @FXML
    private TextArea taAgentOutput;

    @FXML
    private TextField tfBankIP, tfAuctionCentralIP;

    @FXML
    private ListView lvItems;

    @FXML
    private TextField tfBidAmount;

    @FXML
    private GridPane gpBoughtItems;

    private ArrayList<Item> boughtItems;

    Item currentSelectedItem;

    private ArrayList<Item> itemsAsList;
    private ArrayList<AuctionHouse> listOfAHs;
    private ObservableList<String> observableItems;


    Agent agent; // Inside class that keeps account information and item information
    Client client; // Wrapper class for agent that opens sockets and communicates for agent

    /**
     * initialize()
     * Initializes the Agent Controller.
     */
    @FXML
    private void initialize()
    {
        itemsAsList = new ArrayList<>();
        client = new Client(true, Main.askName(), taAgentOutput);
        listOfAHs = new ArrayList<>();
        observableItems = FXCollections.observableArrayList();
        boughtItems = new ArrayList<>();


        // Initializing the user's GUI with a dust bunny in their inventory
        displayBoughtItem(new Item("Dust Bunny", "DustBunny.png", 0.0));


        agent = client.getAgent();

        Thread clientListeningThread = new Thread(new Runnable() {
            @Override
            public void run()
            {
                while(true)
                {
                    // Don't delete this line, this thread gets stopped(garbage collected) without running clientListening()
                    System.out.print("");

                    // If the client isn't already listening, but is connected to the AC, start listening for msg's.
                    if(!Client.isListening() && Client.getAcConnected())
                    {
                        try { client.clientListening(); }// This will listen for messages forever
                        catch(IOException e) { System.out.println(e.getMessage()); }
                        catch(ClassNotFoundException e) { System.out.println(e.getMessage()); }
                    }
                }
            }
        });

        clientListeningThread.start();


        lvItems.getSelectionModel().selectedItemProperty().addListener((obvList, oldVal, newVal) ->
        {
            setCurrentSelectedItem(newVal.toString());
        });

        update();

    }


    /**
     * update()
     * Method that will constantly be updating the UI to the user. Runs on an executor thread.
     */
    private void update()
    {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(new Runnable()
        {
            @Override
            public void run()
            {
//                // Getting the latest list of auction houses that are up and updates the item list
//                if(Client.getAcConnected()) client.updateListOfAHs();

                // Platform syncs this command with the UI, fixes javafx thread bugs
                Platform.runLater(() -> {
                    // If the client has connected to the bank and ac already, update your items
                    if(Client.getAcConnected())
                    {
                        client.updateListOfAHs();
                        updateItemList();
                    }
                    lblBalance.setText(String.valueOf(agent.getAccountBalance()));
                });
            }
        }, 0, 150, TimeUnit.MILLISECONDS);
    }

    /**
     * updateItemList()
     *
     * Updates the GUI with the current list of items from each auction house.
     */
    private void updateItemList()
    {
        listOfAHs = agent.getAuctionHouses();
        itemsAsList.clear();
        //observableItems.clear();


        for(int i = 0; i < listOfAHs.size(); ++i)
        {
            HashMap<Integer, Item> ahItems = listOfAHs.get(i).getItems();
            itemsAsList.addAll(ahItems.values());
            //itemsAsList = new ArrayList<Item>(ahItems.values());
        }

        if(observableItems.size() > itemsAsList.size())
        {
            observableItems.clear();
        }

        for(int i = 0; i < itemsAsList.size(); ++i)
        {
            System.out.println("Index: " + i + " " + itemsAsList.get(i).getItemName());
            if(!itemsAsList.contains(itemsAsList.get(i)))
            {
                itemsAsList.add(itemsAsList.get(i));
            }
            if(!observableItems.contains(itemsAsList.get(i).toString()))
            {
                observableItems.add(itemsAsList.get(i).toString());
            }
        }




        if(observableItems != null) lvItems.setItems(observableItems); //lvItems.getItems().addAll(observableItems);  //setItems(observableItems);

    }


    /**
     * btnWithdraw()
     *
     * On action method for the withdraw button on the GUI.
     */
    @FXML
    private void btnWithdraw()
    {
        taAgentOutput.appendText("Submitted withdraw request to bank for: " + tfBidAmount.getText() + "\n");
        client.withdraw(Double.valueOf(tfBidAmount.getText()), agent);
    }

    /**
     * setCurrentSelectedItem()
     *
     * This is called by the change listener on the listview. When a new item is selected, the item inside the
     * controller is updated. This can be used for various item updates.
     *
     * @param itemString The string of the item appearing on the listview.
     */
    private void setCurrentSelectedItem(String itemString)
    {
        if(itemString != null && !itemsAsList.isEmpty())
        {
            for(int i = 0; i < itemsAsList.size(); ++i)
            {
                if(itemString.equals(itemsAsList.get(i).toString()))
                {
                    currentSelectedItem = itemsAsList.get(i);
                    break;
                }
            }
        }

    }

    /**
     * placeBid()
     * Handler for user clicking that they want to place a bid.
     */
    @FXML
    private void btnPlaceBid()
    {
        if(currentSelectedItem != null)
        {
            Double bidAmount = Double.valueOf(tfBidAmount.getText());
            taAgentOutput.appendText("Placing bid for " + bidAmount + " on item:\n" + currentSelectedItem.toString() + "\n");
            client.placeAHBid(bidAmount, agent.getBiddingKey(), currentSelectedItem);
        }
        else
        {
            taAgentOutput.appendText("Please select an item before placing a bid.\n");
        }

    }

    @FXML
    private void btnConnectLocalhost()
    {
        client.connectLocalhost();
    }

    @FXML
    private void btnConnectBank()
    {
        client.setBankHostname(tfBankIP.getText());
    }

    @FXML
    private void btnConnectAC()
    {
        client.setAcHostname(tfAuctionCentralIP.getText());
    }

    /**
     * addImageToGUI()
     *
     * Useful for adding images to GUI when an agent buys an item.
     *
     */
    private void displayBoughtItem(Item item)//      String imgPath, String itemName)
    {
        Image imageToAdd = new Image(getClass().getResource(item.getImgPath()).toExternalForm());
        ImageView imgView = new ImageView(imageToAdd);
        boughtItems.add(item);
        int row = boughtItems.size() / 10;
        int column = boughtItems.size() % 10;
        gpBoughtItems.add(imgView, column, row);
    }



}
