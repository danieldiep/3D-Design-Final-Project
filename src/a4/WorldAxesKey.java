package a4;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class WorldAxesKey extends AbstractAction {
    private Starter starter;

    public WorldAxesKey(Starter s){
        this.starter = s;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        starter.axesToggle();
    }
}
