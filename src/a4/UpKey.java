package a4;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class UpKey extends AbstractAction {
    CameraControl camera;

    public UpKey(CameraControl c){
        camera = c;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        camera.upKey();
    }
}
