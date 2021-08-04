package a4;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ForwardKey extends AbstractAction {
    CameraControl camera;

    public ForwardKey(CameraControl c){
        camera = c;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        camera.forwardKey();
    }
}
