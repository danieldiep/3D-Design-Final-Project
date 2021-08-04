package a4;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class LeftKey extends AbstractAction {
    CameraControl camera;

    public LeftKey(CameraControl c){
        camera = c;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        camera.leftKey();
    }
}
