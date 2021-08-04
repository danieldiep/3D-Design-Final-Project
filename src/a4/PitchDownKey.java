package a4;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class PitchDownKey extends AbstractAction {
    CameraControl camera;

    public PitchDownKey(CameraControl c){
        camera = c;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        camera.pitchDownKey();
    }
}
