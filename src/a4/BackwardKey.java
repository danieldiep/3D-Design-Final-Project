package a4;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class BackwardKey extends AbstractAction {
    CameraControl camera;

    public BackwardKey(CameraControl c){
        camera = c;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        camera.backwardKey();
    }
}
