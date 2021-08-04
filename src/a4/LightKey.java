package a4;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class LightKey extends AbstractAction {
    private Starter starter;

    public LightKey(Starter s) {
        this.starter= s;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        starter.movableLightToggle();
    }
}
