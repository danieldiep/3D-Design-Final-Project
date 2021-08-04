package a4;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class PitchUpKey extends AbstractAction {
    CameraControl camera;

    public PitchUpKey(CameraControl c){
        camera = c;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        camera.pitchUpKey();
    }
}
