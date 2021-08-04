package a4;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class DownKey extends AbstractAction {
    CameraControl camera;

    public DownKey(CameraControl c){
        camera = c;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        camera.downKey();
    }
}
