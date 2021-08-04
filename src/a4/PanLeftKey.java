package a4;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class PanLeftKey extends AbstractAction {
    CameraControl camera;

    public PanLeftKey(CameraControl c){
        camera = c;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        camera.panLeftKey();
    }
}
