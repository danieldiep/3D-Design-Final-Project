package a4;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class FogKey extends AbstractAction {
    private Starter program;

    public FogKey(Starter p){program = p;}
    @Override
    public void actionPerformed(ActionEvent e) {
        program.fogToggle();
    }
}
