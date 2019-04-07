import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;

/**
 * This is a special timer class for Items that are at AuctionHouses. The time length is 30s, and the
 * event can be set via the setOnFinished method. It takes an item so that it can be referenced later.
 */
public class AuctionTimer
{
    private static final Integer STARTTIME = 30;
    private Timeline timeline;
    private IntegerProperty timeSeconds;
    private final Item item; //the item in AH for which this timer is set.

    /**
     * Auction Timer Constructor
     * @param item
     */
    AuctionTimer(Item item)
    {
        this.item = item;
        timeline = new Timeline();
        timeSeconds = new SimpleIntegerProperty(STARTTIME);
        timeline.setCycleCount(STARTTIME);
        timeline.setCycleCount(1);
        timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(STARTTIME+1), new KeyValue(timeSeconds, 0)));
    }
    
    /**
     * @return the Item that was stored in this timer on it's instantiation.
     */
    public Item getItem()
    {
        return item;
    }
    
    /**
     * Starts the TimeLine
     */
    public void playFromStart()
    {
        timeline.playFromStart();
    }
    
    /**
     * Stops the Timeline
     */
    public void stop()
    {
        timeline.stop();
    }
    
    /**
     * Call this to set the timeline's setOnFinished value
     * @param value The action to be executed at the conclusion of the timeline.
     */
    public void setOnFinished(EventHandler<ActionEvent> value)
    {
        timeline.setOnFinished(value);
    }
}
