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
import viewer.Popup;

/**
 * FXML Controller class
 *
 * @author Ludek
 */
public class HelpController extends Popup {

    @Override
    public void ready() {}

     @FXML
    private void closeDebug(){
        getBridgeReference().getDetector().setShowDebugData(false);
        closePopup();
    }
    
}
