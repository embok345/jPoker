/*
 * Copyright (C) 2018 Em Poulter <em@poulter.space>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package space.poulter.em.poker.client;

import java.io.Serializable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import space.poulter.em.poker.PokerTableData;

/**
 *
 * @author Em Poulter
 */
public class PokerTableDataClient extends PokerTableData implements Serializable {
    
    private IntegerProperty tableIDProp;
    private IntegerProperty maxHandsProp;
    private IntegerProperty potProp;
    
    public PokerTableDataClient() {
        super();
    }
    
    public PokerTableDataClient(PokerTableData dat) {
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
        updateProperties();
    }
    
    @Override
    public void init(int id, int hands) {
        super.init(id, hands);
        updateProperties();
    }
    
    @Override
    public void setTableID(Integer value) { 
        tableIDProperty().set(value); 
        super.setTableID(value);
    }
    @Override
    public Integer getTableID() { return tableIDProperty().get(); }
    public IntegerProperty tableIDProperty() { 
        if (tableIDProp == null) tableIDProp = new SimpleIntegerProperty(this, "tableID");
        return tableIDProp; 
    }

    @Override
    public void setMaxHands(Integer value) { 
        maxHandsProperty().set(value); 
        super.setMaxHands(value);
    }
    @Override
    public Integer getMaxHands() { return maxHandsProperty().get(); }
    public IntegerProperty maxHandsProperty() {
        if(maxHandsProp == null) maxHandsProp = new SimpleIntegerProperty(this, "maxHands");
        return maxHandsProp;
    }
    
    @Override
    public void setPot(Integer value) {
        potProperty().set(value);
        super.setPot(value);
    }
    @Override
    public Integer getPot() { return potProperty().get(); }
    public IntegerProperty potProperty() {
        if(potProp == null) potProp = new SimpleIntegerProperty(this, "pot");
        return potProp;
    }
    
    public void updateProperties() {
        setTableID(super.getTableID());
        setMaxHands(super.getMaxHands());
        setPot(super.getPot());
    }
}
