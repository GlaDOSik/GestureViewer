/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package viewer.VC;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import viewer.Popup;

/**
 FXML Controller class

 @author Ludek
 */
public class DebugWindowController extends Popup{

    @Override
    public void ready(){ 
    }
    
    
    @FXML
    private ImageView debugImg1;
    @FXML
    private ImageView debugImg2;
    
    @FXML
    private void closeDebug(){
        getBridgeReference().getDetector().setShowDebugData(false);
        closePopup();
    }
    
    public ImageView getImageView1(){
        return debugImg1;
    }
     public ImageView getImageView2(){
        return debugImg2;
    }
    
      @FXML
    private void moveWindow(MouseEvent event) {
        if (!getStage().isMaximized() && !getStage().isFullScreen()) {
            moveDragged(event, true);
        }
    }

    @FXML
    private void startMoveWindow(MouseEvent event) {
        if (!getStage().isMaximized() && !getStage().isFullScreen()) {
            moveStart(event);
        }
    }       
    
}
