package viewer.VC;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import viewer.Bridge;
import viewer.BridgeComponent;
import viewer.DragHandler;
import viewer.Popup;
import viewer.PressedHandler;
import viewer.ScrollHandler;
import viewer.interfaces.IControllerViewState;
import viewer.interfaces.IViewState;

public class PdfTController extends Popup implements IControllerViewState {

    @Override
    public void ready() {
        dh = new DragHandler(scrlPane);
        ph = new PressedHandler(scrlPane);
        sh = new ScrollHandler(imageView, scrlPane, stackPane);

        scrlPane.prefWidthProperty().bind(((MainWindowController) getBridgeReference().getController(Bridge.View.MainWindowP)).getStage().widthProperty());
        imageView.fitWidthProperty().bind(((MainWindowController) getBridgeReference().getController(Bridge.View.MainWindowP)).getStage().widthProperty().subtract(150));

        stackPane.setOnScroll(sh);
        stackPane.setOnMousePressed(ph);
        stackPane.setOnMouseDragged(dh);

        stage = new Stage();
    }

    @FXML
    private ScrollPane scrlPane;
    @FXML
    private StackPane stackPane;
    @FXML
    private ImageView imageView;
    @FXML
    private TextField txtfCurrentPage;
    @FXML
    private Label lblPages;
    @FXML
    private ToggleButton toggleFullscreen;

    private PDDocument document;
    private PDFRenderer renderer;
    private int currentPage = 0;
    DragHandler dh;
    PressedHandler ph;
    ScrollHandler sh;
    Stage stage;

    @Override
    public void addTabState(Tab tab, File[] filesInDirectory, int fileIndex, int offset) {
        MainWindowController mainGUI = (MainWindowController) getBridgeReference().getController(Bridge.View.MainWindowP);
        VariantState state = mainGUI.addVariantTabState(tab);

        state.filesInDirectory = filesInDirectory;
        state.fileIndex = fileIndex;
        state.offset = offset;
    }

    @Override
    public IViewState getState(Tab tab) {
        MainWindowController mainGUI = (MainWindowController) getBridgeReference().getController(Bridge.View.MainWindowP);
        return mainGUI.getVariantTabState(tab);
    }

    @Override
    public void removeTabState(Tab tab) {
        MainWindowController mainGUI = (MainWindowController) getBridgeReference().getController(Bridge.View.MainWindowP);
        mainGUI.removeVariantTabState(tab);

        closeDocument();
    }

    @Override
    public void saveState(Tab sourceTab) {
        MainWindowController mainGUI = (MainWindowController) getBridgeReference().getController(Bridge.View.MainWindowP);
        VariantState state = mainGUI.getVariantTabState(sourceTab);
        state.currentPage = currentPage;
        state.vValue = scrlPane.getVvalue();
    }

    @Override
    public void loadState(Tab sourceTab) {
        if (document != null) {
            try {
                document.close();
            }
            catch (IOException ex) {
                ((MainWindowController) getBridgeReference().getController(Bridge.View.MainWindowP)).setViewerStatus("PDF dokument se nepodařil zavřít");
                return;
            }
        }

        MainWindowController mainGUI = (MainWindowController) getBridgeReference().getController(Bridge.View.MainWindowP);
        VariantState state = mainGUI.getVariantTabState(sourceTab);
        try {
            document = PDDocument.load(state.filesInDirectory[state.fileIndex]);
        }
        catch (IOException ex) {
            ((MainWindowController) getBridgeReference().getController(Bridge.View.MainWindowP)).setViewerStatus("PDF dokument se nepodařil načíst");
            return;
        }
        renderer = new PDFRenderer(document);
        currentPage = state.currentPage;
        lblPages.setText("/" + document.getNumberOfPages());
        renderCurrentPage();
        scrlPane.setVvalue(state.vValue);
        mainGUI.setFilesCount(state.filesInDirectory, state.fileIndex, state.offset, false);

    }

    @FXML
    private void nextPage() {
        currentPage++;
        if (currentPage > document.getNumberOfPages() - 1) {
            currentPage = 0;
        }
        renderCurrentPage();
    }

    @FXML
    private void prevPage() {
        currentPage--;
        if (currentPage < 0) {
            currentPage = document.getNumberOfPages() - 1;
        }
        renderCurrentPage();
    }

    @Override
    public int getNumberOfStates() {
        MainWindowController mainGUI = (MainWindowController) getBridgeReference().getController(Bridge.View.MainWindowP);
        return mainGUI.getNumberOfStates();
    }

    private void renderCurrentPage() {
        try {
            Image fxi = SwingFXUtils.toFXImage(renderer.renderImageWithDPI(currentPage, 250), null);
            imageView.setImage(fxi);
        }
        catch (IOException ex) {
            ((MainWindowController) getBridgeReference().getController(Bridge.View.MainWindowP)).setViewerStatus("Nepodařila se vykreslit strana dokumentu");
            return;
        }

        scrlPane.setVvalue(0.0);
        txtfCurrentPage.setText(Integer.toString(currentPage + 1));
    }

    private void closeDocument() {
        if (document != null) {
            try {
                document.close();
            }
            catch (IOException ex) {
                Logger.getLogger(PdfTController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @FXML
    private void onEnter(ActionEvent e) {
        //parsovat hodnotu, vykreslit stranu
        int page;
        try {
            page = Integer.parseInt(txtfCurrentPage.getText());
        }
        catch (NumberFormatException ex) {
            return;
        }
        page = Math.max(1, Math.min(page, document.getNumberOfPages()));
        currentPage = page - 1;
        renderCurrentPage();
    }

    @FXML
    private void setFullscreen() {
        MainWindowController mainGUI = (MainWindowController) getBridgeReference().getController(Bridge.View.MainWindowP);
        if (toggleFullscreen.isSelected()) {
            scrlPane.prefWidthProperty().unbind();
            imageView.fitWidthProperty().unbind();

            mainGUI.getSelestedTab().setContent(null);
            mainGUI.addPopUp(Bridge.View.PdfT, mainGUI.getSelestedTab().getText(), false, false, null);
            scrlPane.prefWidthProperty().bind(getStage().widthProperty());
            imageView.fitWidthProperty().bind(getStage().widthProperty().subtract(150));
            
            getStage().setFullScreenExitHint("");
            getStage().setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
            getStage().setFullScreen(true);
            
        }
        else {
            scrlPane.prefWidthProperty().unbind();
            imageView.fitWidthProperty().unbind();
            getStage().setFullScreen(false);
            getStage().close();
            getStage().setScene(null);
            mainGUI.getSelestedTab().setContent(getViewRoot());
            setStage(null);

            scrlPane.prefWidthProperty().bind(((MainWindowController) getBridgeReference().getController(Bridge.View.MainWindowP)).getStage().widthProperty());
            imageView.fitWidthProperty().bind(((MainWindowController) getBridgeReference().getController(Bridge.View.MainWindowP)).getStage().widthProperty().subtract(150));
        }
    }

}
