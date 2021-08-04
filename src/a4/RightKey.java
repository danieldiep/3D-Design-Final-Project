package a4;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class RightKey extends AbstractAction {
    CameraControl camera;

    public RightKey(CameraControl c){
        camera = c;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        camera.rightKey();
    }
}
