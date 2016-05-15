package viewer.VC;

import java.io.File;
import java.util.HashMap;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.commons.io.FilenameUtils;
import viewer.Bridge;
import viewer.BridgeComponent;
import viewer.Popup;
import viewer.interfaces.IControllerViewState;

public class MainWindowController extends Popup {

    @Override
    public void ready() {
        tabStates = new HashMap<>();
    }

    @FXML
    private TabPane tabPane;
    @FXML
    private Button btnMaximize;
    @FXML
    private Button btnResize;
    @FXML
    private Button btnDetectorStart;
    @FXML
    private Label lblViewerStatus;
    @FXML
    private Label lblFilesCount;
    @FXML
    private ImageView gestureIcon;

    private byte windowMode = 1;
    private HashMap<Tab, VariantState> tabStates;

    @FXML
    public void prevFile() {
        switchFile(false);
    }

    @FXML
    public void nextFile() {
        switchFile(true);
    }

    private void switchFile(boolean isNextFile) {
        //najít stav, změnit index souboru, případně změnit obsah záložky
        //nad kontrolérem záložky zavolat loadState
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == null) {
            return;
        }
        VariantState state = tabStates.get(selectedTab);
        if (isNextFile) {
            state.fileIndex++;
            if (state.fileIndex >= state.filesInDirectory.length - state.offset) {
                state.fileIndex = 0;
            }
        }
        else {
            state.fileIndex--;
            if (state.fileIndex < 0) {
                state.fileIndex = state.filesInDirectory.length - state.offset - 1;
            }
        }

        selectedTab.setText(state.filesInDirectory[state.fileIndex].getName());

        //pokud je potřeba změnit prohlížeč v záložce, změní ho
        String fileType = FilenameUtils.getExtension(state.filesInDirectory[state.fileIndex].getAbsolutePath());
        if (fileType.equals("jpg") || fileType.equals("jpeg") || fileType.equals("bmp") || fileType.equals("gif") || fileType.equals("png")) {
            //pokud není v aktuálním tabu obrázek, ale aktuální soubor obrázek je
            if (!selectedTab.getUserData().toString().equals(Bridge.View.ImageT.toString())) {
                //saveState uloží stav prohlížeče, případně ukončí přehrávání médií, atd.
                ((IControllerViewState) getBridgeReference().getController(Bridge.View.valueOf(selectedTab.getUserData().toString()))).saveState(selectedTab);
                selectedTab.setUserData(Bridge.View.ImageT);
                selectedTab.setContent(null);
                selectedTab.setContent(getBridgeReference().getController(Bridge.View.ImageT).getViewRoot());
                System.out.println("prepinam na obrazek");
            }
        }
        else if (fileType.equals("mp3") || fileType.equals("wav") || fileType.equals("mp4") || fileType.equals("m4a") || fileType.equals("m4v") || fileType.equals("flv") || fileType.equals("fxm") || fileType.equals("aiff") || fileType.equals("aif")) {
            //pokud není v aktuálním tabu médium, ale aktuální soubor médium je
            if (!selectedTab.getUserData().toString().equals(Bridge.View.MediaT.toString())) {
                ((IControllerViewState) getBridgeReference().getController(Bridge.View.valueOf(selectedTab.getUserData().toString()))).saveState(selectedTab);
                selectedTab.setUserData(Bridge.View.MediaT);
                selectedTab.setContent(null);
                selectedTab.setContent(getBridgeReference().getController(Bridge.View.MediaT).getViewRoot());
                System.out.println("prepinam na media");
            }
        }
        else if (fileType.equals("pdf")) {
            //pokud není v aktuálním tabu pdf, ale aktuální soubor pdf je
            if (!selectedTab.getUserData().toString().equals(Bridge.View.PdfT.toString())) {
                ((IControllerViewState) getBridgeReference().getController(Bridge.View.valueOf(selectedTab.getUserData().toString()))).saveState(selectedTab);
                selectedTab.setUserData(Bridge.View.PdfT);
                selectedTab.setContent(null);
                selectedTab.setContent(getBridgeReference().getController(Bridge.View.PdfT).getViewRoot());
                System.out.println("prepinam na pdf");
            }
        }

        //loadState nahraje nový soubor do vhodného prohlížeče
        ((IControllerViewState) getBridgeReference().getController(Bridge.View.valueOf(selectedTab.getUserData().toString()))).loadState(selectedTab);
    }

    //soubory nepodporovaného typu jsou null, vybranemu souboru je přidělen index fileIndex
    //každá záložka má jeden stav uložen v této třídě - stav obsahuje vše - informace o souborech přiřazených k záložce, přiblížení pdf dokumentu, atd.
    @FXML
    private void openFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Vybrat soubor");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Všechny soubory", "*"),
                new FileChooser.ExtensionFilter("Obrázky", "*.jpg", "*.jpeg", "*.bmp", "*.gif", "*.png"),
                new FileChooser.ExtensionFilter("Dokumenty", "*.pdf"),
                new FileChooser.ExtensionFilter("Multimediální soubory", "*.mp3", "*.waw", "*.mp4", "*.m4a", "*.m4v", "*.flv", "*.fxm", "*.aiff", "*.aif")
        );
        File file = fileChooser.showOpenDialog(((Popup) getBridgeReference().getController(Bridge.View.MainWindowP)).getStage());
        if (file == null) {
            return;
        }

        String fileType = FilenameUtils.getExtension(file.getAbsolutePath());
        File parentDirectory = new File(file.getParent());
        File[] filesInDirectory = parentDirectory.listFiles();

        //načte všechny podporované soubory ze složky
        //filesInDirectory obsahuje na konci null - ty přeskočit
        int offset = 0;
        int fileIndex = 0;
        for (int i = 0; i < filesInDirectory.length; i++) {
            if (testSupportedFiles(FilenameUtils.getExtension(filesInDirectory[i].getAbsolutePath())) && filesInDirectory[i].canRead()) {
                if (file.getName().equals(filesInDirectory[i].getName())) {
                    fileIndex = i - offset;
                }
                filesInDirectory[i - offset] = filesInDirectory[i];
            }
            else {
                offset++;
            }
        }
        //filesInDirectory obsahuje i nepodporované soubory, podporované jsou seřazeny, 
        //offset je část konce pole, která by se nesměla používat

        if (fileType.equals("jpg") || fileType.equals("jpeg") || fileType.equals("bmp") || fileType.equals("gif") || fileType.equals("png")) {
            //načíst obrázek - pokud je potřeba, jiný tab si ho uloží při vytvoření
            createTab(Bridge.View.ImageT, file.getName(), true, filesInDirectory, fileIndex, offset);
        }
        else if (fileType.equals("mp3") || fileType.equals("wav") || fileType.equals("mp4") || fileType.equals("m4a") || fileType.equals("m4v") || fileType.equals("flv") || fileType.equals("fxm") || fileType.equals("aiff") || fileType.equals("aif")) {
            createTab(Bridge.View.MediaT, file.getName(), true, filesInDirectory, fileIndex, offset);
        }
        else if (fileType.equals("pdf")) {
            createTab(Bridge.View.PdfT, file.getName(), true, filesInDirectory, fileIndex, offset);
        }
        else {
            lblViewerStatus.setText("Nepodporovaný typ souboru");
            lblFilesCount.setText("0/0");
        }
    }

    private boolean testSupportedFiles(String fileExtension) {
        switch (fileExtension) {
            case "jpg":
                return true;
            case "jpeg":
                return true;
            case "bmp":
                return true;
            case "gif":
                return true;
            case "png":
                return true;
            case "mp3":
                return true;
            case "wav":
                return true;
            case "mp4":
                return true;
            case "m4a":
                return true;
            case "m4v":
                return true;
            case "flv":
                return true;
            case "fxm":
                return true;
            case "aiff":
                return true;
            case "aif":
                return true;
            case "pdf":
                return true;
            default:
                return false;
        }
    }

    public Tab createTab(Bridge.View viewT, String name, boolean isSelected, File[] filesInDirectory, int fileIndex, int offset) {
        //New tab
        Tab newTab = new Tab(name);
        newTab.setUserData(viewT);

        BridgeComponent bridgeComponent = getBridgeReference().getController(viewT);

        //if controller implements IControllerViewState, it's not singleton and it's working with states
        if (bridgeComponent instanceof IControllerViewState) {
            newTab.setOnSelectionChanged(new EventHandler<Event>() {
                @Override
                public void handle(Event event) {
                    Tab switchedTab = (Tab) event.getSource();
                    IControllerViewState controllerViewState = (IControllerViewState) getBridgeReference().getController((Bridge.View) switchedTab.getUserData());

                    if (controllerViewState.getState(switchedTab).isActive()) {
                        controllerViewState.getState(switchedTab).setActiveState(false);
                        controllerViewState.saveState(switchedTab);
                        System.out.println("ukladam");
                    }
                    else {
                        controllerViewState.getState(switchedTab).setActiveState(true);
                        controllerViewState.loadState(switchedTab);
                        switchedTab.setContent(null);
                        switchedTab.setContent(((BridgeComponent) controllerViewState).getViewRoot());
                        System.out.println("nahravam");
                    }
                }
            });

            newTab.setOnCloseRequest(new EventHandler<Event>() {
                @Override
                public void handle(Event event) {
                    //pokud stav není uložen, zkonzumovat event a vyhodit popup potvrzení
                    Tab removedTab = (Tab) event.getSource();
                    removedTab.setOnCloseRequest(null);
                    removedTab.setOnSelectionChanged(null);
                    removedTab.setContent(null);
                    IControllerViewState controllerViewState = (IControllerViewState) getBridgeReference().getController((Bridge.View) removedTab.getUserData());
                    controllerViewState.removeTabState(removedTab);
                }
            });
            ((IControllerViewState) bridgeComponent).addTabState(newTab, filesInDirectory, fileIndex, offset);
        }
        else {
            newTab.setOnCloseRequest(new EventHandler<Event>() {
                @Override
                public void handle(Event event) {
                    Tab removedTab = (Tab) event.getSource();
                    removedTab.setOnCloseRequest(null);
                    removedTab.setContent(null);
                }
            });
        }
        newTab.setContent(bridgeComponent.getViewRoot());

        tabPane.getTabs().add(newTab);
        if (isSelected) {
            tabPane.getSelectionModel().select(newTab);
        }
        return newTab;
    }

    public void addPopUp(Bridge.View viewP, String title, boolean onTop, boolean useParameterStage, Stage stage) {
        Popup popup = (Popup) getBridgeReference().getController(viewP);
        if (popup.getStage() == null) {
            if (useParameterStage) {
                popup.setStage(stage);
                popup.getStage().initStyle(StageStyle.UNDECORATED);
            }
            else {
                popup.setStage(new Stage(StageStyle.UNDECORATED));
            }

            popup.getStage().setTitle(title);
            popup.getStage().setScene(new Scene(popup.getViewRoot()));
            popup.getStage().getScene().getStylesheets().add("viewer/skins/DarkDefault/DefaultSkin.css");
            popup.getStage().setWidth(((Pane) popup.getViewRoot()).getPrefWidth());
            popup.getStage().setHeight(((Pane) popup.getViewRoot()).getPrefHeight());
            popup.getStage().setAlwaysOnTop(onTop);
        }
        popup.showPopup();
    }

    public VariantState getVariantTabState(Tab tab) {
        return tabStates.get(tab);
    }

    public VariantState addVariantTabState(Tab tab) {
        VariantState state = new VariantState();
        tabStates.put(tab, state);
        return state;
    }

    public void removeVariantTabState(Tab tab) {
        tabStates.remove(tab);
    }

    public int getNumberOfStates() {
        return tabStates.size();
    }

    @FXML
    private void resizeWindow(MouseEvent event) {
        resize(event);
    }

    @FXML
    private void closeMainWindow() {
        if (getBridgeReference().getDetector().isDetectorActive()) {
            getBridgeReference().getDetector().stop();
        }
        if (!((DebugWindowController) getBridgeReference().getController(Bridge.View.DebugWindowP)).isClosed()) {
            ((DebugWindowController) getBridgeReference().getController(Bridge.View.DebugWindowP)).closePopup();
        }

        for (int i = 0; i < tabPane.getTabs().size(); i++) {
            tabPane.getTabs().get(i).getOnCloseRequest().handle(new Event(tabPane.getTabs().get(i), tabPane.getTabs().get(i), EventType.ROOT));
        }
        tabPane.getTabs().clear();

        closePopup();
    }

    @FXML
    private void maximizeMainWindow() {
        if (windowMode == 1) {
            getStage().setMaximized(true);
            getStage().setHeight(Screen.getPrimary().getVisualBounds().getHeight());
            btnResize.setVisible(false);
            windowMode = 2;
            btnMaximize.setStyle("-fx-graphic: url('viewer/skins/DarkDefault/button-fullscreen-hover.png');");
        }
        else if (windowMode == 2) {
            getStage().setHeight(Screen.getPrimary().getBounds().getHeight());
            windowMode = 3;
            btnMaximize.setStyle("-fx-graphic: url('viewer/skins/DarkDefault/button-unmaximize-hover.png');");
        }
        else {
            getStage().setMaximized(false);
            btnResize.setVisible(true);
            windowMode = 1;
            btnMaximize.setStyle("-fx-graphic: url('viewer/skins/DarkDefault/button-maximize-hover.png');");
        }
    }

    @FXML
    private void minimizeMainWindow() {
        if (getStage().isIconified()) {
            getStage().setIconified(false);
        }
        else {
            getStage().setIconified(true);
        }
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

    @FXML
    private void detectorStartStop() {
        if (!getBridgeReference().getDetector().isDetectorActive()) {
            btnDetectorStart.setStyle("-fx-graphic: url('viewer/skins/DarkDefault/button-detector-stop.png');");
            lblViewerStatus.setText("Přesuňte se prosím mimo záběr kamery. Učím se pozadí.");
            getBridgeReference().getDetector().start();
        }
        else {
            btnDetectorStart.setStyle("-fx-graphic: url('viewer/skins/DarkDefault/button-detector-start.png');");
            lblViewerStatus.setText("Detektor vypnut");
            getBridgeReference().getDetector().stop();
        }
    }

    public void setViewerStatus(String text) {
        lblViewerStatus.setText(text);
    }

    @FXML
    private void openDebugWindow() {
        addPopUp(Bridge.View.DebugWindowP, "Debugovací okno", true, false, null);
        getBridgeReference().getDetector().setShowDebugData(true);
    }

    public void setFilesCount(File[] filesInDirectory, int fileIndex, int offset, boolean clearLabel) {
        if (!clearLabel) {
            int numberOfSupportedFiles = filesInDirectory.length - offset;
            lblFilesCount.setText(fileIndex + 1 + "/" + numberOfSupportedFiles + "  " + filesInDirectory[fileIndex].getParent());
        }
        else {
            lblFilesCount.setText("0/0");
        }
    }

    @FXML
    public void closeSelectedTab() {
        Tab removedTab = tabPane.getSelectionModel().getSelectedItem();
        if (removedTab != null) {
            removedTab.getOnCloseRequest().handle(new Event(removedTab, removedTab, EventType.ROOT));
            tabPane.getTabs().remove(removedTab);
            //setFilesCount(null, 0, 0, true);
        }
    }

    public Tab getSelestedTab() {
        return tabPane.getSelectionModel().getSelectedItem();
    }

    public void setKeyEventHandler() {
        getStage().getScene().addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent evt) {
                if (evt.getCode().equals(KeyCode.ESCAPE)) {
                    btnDetectorStart.setStyle("-fx-graphic: url('viewer/skins/DarkDefault/button-detector-start.png');");
                    lblViewerStatus.setText("Detektor vypnut");
                    getBridgeReference().getDetector().stop();
                }
            }
        });

        /*getStage().getScene().setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.ESCAPE) {
                    btnDetectorStart.setStyle("-fx-graphic: url('viewer/skins/DarkDefault/button-detector-start.png');");
                    lblViewerStatus.setText("Detektor vypnut");
                    getBridgeReference().getDetector().stop();
                }
            }
        });*/
    }

}
