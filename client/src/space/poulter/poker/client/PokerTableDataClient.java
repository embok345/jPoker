package space.poulter.poker.client;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.jetbrains.annotations.NotNull;
import space.poulter.poker.PokerTableData;

/**
 * Container for all of the data at the poker table, for the GUI. This includes the IntegerPropertys for various
 * getRanks, which we bind several text fields on the GUI to.
 *
 * @author Em Poulter <em@poulter.space>
 */
public class PokerTableDataClient extends PokerTableData {

    /* The IntegerProperty getRanks representing the tableID, the max # of hands, and the current pot total
     * TODO I feel other getRanks should be here, each hands total etc.
     */
    private IntegerProperty tableIDProp;
    private IntegerProperty maxHandsProp;
    private IntegerProperty potProp;

    /**
     * Create a new PokerTableDataClient from all of the information in dat. This just copies all of the getRanks
     * from dat into this, and then updates the properties for the graphics.
     *
     * @param dat The PokerTableData to be copied.
     */
    PokerTableDataClient(@NotNull PokerTableData dat) {
        /* Copy all of the data
         * TODO surely there is a better way to do this */
        super.setPlayers(dat.getPlayerAndIndex());
        super.setGameRunning(dat.isGameRunning());
        super.setStageOfPlay(dat.getStageOfPlay());
        super.setPlayersInHand(dat.getPlayersInHand());
        super.setPot(dat.getPot());
        super.setBet(dat.getBet());
        super.setDealer(dat.getDealer());
        super.setSmallBlind(dat.getSmallBlind());
        super.setBigBlind(dat.getBigBlind());
        super.setBoardCards(dat.getBoard());
        super.setTableID(dat.getTableID());
        super.setMaxHands(dat.getMaxHands());

        /* Update the properties for the graphics */
        updateProperties();
    }

    /**
     * Get the table ID, as an int.
     *
     * @return The table ID.
     */
    @Override
    public Integer getTableID() {
        return tableIDProperty().get(); //Note we get from the IntegerProperty, rather than the Integer, hence the override
    }

    /**
     * Update the table ID property to the new value.
     *
     * @param value The new table ID.
     */
    @Override
    public void setTableID(Integer value) {
        tableIDProperty().set(value);
        super.setTableID(value);
    }

    /**
     * Get the table ID, as an IntegerProperty.
     *
     * @return The table ID.
     */
    IntegerProperty tableIDProperty() {
        //If the property is empty, create a new one, with the value of the tableID int.
        if (tableIDProp == null) tableIDProp = new SimpleIntegerProperty(this, "tableID");
        return tableIDProp;
    }

    /**
     * Get the value of the max hands property.
     *
     * @return The value of the max hands Property.
     */
    @Override
    public Integer getMaxHands() {
        return maxHandsProperty().get();
    }

    /**
     * Update the max hands property to the new value.
     *
     * @param value The new value of the max hands property.
     */
    @Override
    public void setMaxHands(Integer value) {
        maxHandsProperty().set(value);
        super.setMaxHands(value);
    }

    /**
     * Get the max hands property.
     *
     * @return The max hands IntegerProperty.
     */
    IntegerProperty maxHandsProperty() {
        if (maxHandsProp == null) maxHandsProp = new SimpleIntegerProperty(this, "maxHands");
        return maxHandsProp;
    }

    /**
     * Get the value of the pot property.
     *
     * @return The value of the pot Property.
     */
    @Override
    public Integer getPot() {
        return potProperty().get();
    }

    /**
     * Update the pot value property to the new value.
     *
     * @param value The new size of the pot.
     */
    @Override
    public void setPot(Integer value) {
        potProperty().set(value);
        super.setPot(value);
    }

    /**
     * Get the pot IntegerProperty.
     *
     * @return The pot IntegerProperty.
     */
    IntegerProperty potProperty() {
        if (potProp == null) potProp = new SimpleIntegerProperty(this, "pot");
        return potProp;
    }

    /**
     * Update all of the IntegerPropertys on the table.
     */
    void updateProperties() {
        setTableID(super.getTableID());
        setMaxHands(super.getMaxHands());
        setPot(super.getPot());
    }
}
