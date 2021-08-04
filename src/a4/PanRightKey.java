package a4;
import javax.swing.*;
import java.awt.event.ActionEvent;

public class PanRightKey extends AbstractAction {
    CameraControl camera;

    public PanRightKey(CameraControl c){
        camera = c;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        camera.panRightKey();
    }
}
