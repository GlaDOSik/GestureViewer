package viewer;

import viewer.detector.DetectorController;
import java.io.IOException;
import java.util.HashMap;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import viewer.VC.DebugWindowController;
import viewer.detector.Detector;
import viewer.VC.MainWindowController;

public class Bridge {

    public Bridge(Stage primaryStage) {        
        controllers = new HashMap<>();
        detector = new Detector(this);
        viewerController = new DetectorController(this, detector);

        loadViewController(View.MainWindowP, "VC/MainWindow.fxml");

        ((MainWindowController) getController(View.MainWindowP)).addPopUp(View.MainWindowP, "Viewer", false, true, primaryStage);
        ((MainWindowController) getController(View.MainWindowP)).setViewerStatus("Prohlížeč otevřen");
         ((MainWindowController) getController(View.MainWindowP)).setKeyEventHandler();
        
        loadViewController(View.DebugWindowP, "VC/DebugWindow.fxml");
        loadViewController(View.ImageT, "VC/ImageT.fxml");
        loadViewController(View.PdfT, "VC/PdfT.fxml");
        loadViewController(View.MediaT, "VC/MediaT.fxml");
        loadViewController(View.HelpP, "VC/Help.fxml");
        
        detector.setImageView(((DebugWindowController) controllers.get(View.DebugWindowP)).getImageView1(), ((DebugWindowController) controllers.get(View.DebugWindowP)).getImageView2());
    }

    public enum View {
        MainWindowP, DebugWindowP, HelpP,
        ImageT, PdfT, MediaT
    }

    private final HashMap<View, BridgeComponent> controllers;
    private final DetectorController viewerController;
    private final Detector detector;

    private void loadViewController(View view, String pathToView) {
        if (!controllers.containsKey(view)) {
            FXMLLoader loader = new FXMLLoader();
            try {
                Parent root = loader.load(getClass().getResource(pathToView).openStream());
                BridgeComponent bridgeComponent = loader.getController();
                bridgeComponent.setBridgeReference(this);
                bridgeComponent.setViewRoot(root);
                controllers.put(view, bridgeComponent);
                bridgeComponent.ready();
            }
            catch (IOException ex) {
                System.out.println("Došlo k chybě při načítání fxml souboru!");
                System.out.println(ex.getMessage());
                ((MainWindowController) getController(View.MainWindowP)).setViewerStatus("Došlo k chybě při načítání fxml souboru!");
            }
        }
    }

    public BridgeComponent getController(View view) {
        return controllers.get(view);
    }

    public Detector getDetector() {
        return detector;
    }

    public DetectorController getViewerController() {
        return viewerController;
    }

}
