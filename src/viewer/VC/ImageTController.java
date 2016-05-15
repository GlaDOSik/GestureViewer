package viewer.VC;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import viewer.Bridge;
import viewer.BridgeComponent;
import viewer.interfaces.IControllerViewState;
import viewer.interfaces.IViewState;

public class ImageTController extends BridgeComponent implements IControllerViewState {

    @Override
    public void ready() {
        imageView.fitWidthProperty().bind(((MainWindowController) getBridgeReference().getController(Bridge.View.MainWindowP)).getStage().widthProperty());
        imageView.fitHeightProperty().bind(((MainWindowController) getBridgeReference().getController(Bridge.View.MainWindowP)).getStage().heightProperty().subtract(160));
    }

    private Image openedImage;
    private InputStream is;

    @FXML
    private ImageView imageView;

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
    }

    @Override
    public void saveState(Tab sourceTab) {
    }

    @Override
    public void loadState(Tab sourceTab) {
        MainWindowController mainGUI = (MainWindowController) getBridgeReference().getController(Bridge.View.MainWindowP);
        VariantState state = mainGUI.getVariantTabState(sourceTab);
        try {
            is = new FileInputStream(state.filesInDirectory[state.fileIndex]);
            openedImage = new Image(is);
            is.close();
        }
        catch (FileNotFoundException ex) {
            mainGUI.setViewerStatus("Obrázek nenalezen");
            mainGUI.setFilesCount(state.filesInDirectory, state.fileIndex, state.offset, true);
            return;
        }
        catch (IOException ex) {
            mainGUI.setViewerStatus("Nepodařilo se zavřít soubor");
            mainGUI.setFilesCount(state.filesInDirectory, state.fileIndex, state.offset, true);
            mainGUI.closeSelectedTab();
            return;
        }
        imageView.setImage(openedImage);
        mainGUI.setFilesCount(state.filesInDirectory, state.fileIndex, state.offset, false);
    }

    @Override
    public int getNumberOfStates() {
        MainWindowController mainGUI = (MainWindowController) getBridgeReference().getController(Bridge.View.MainWindowP);
        return mainGUI.getNumberOfStates();
    }

}
