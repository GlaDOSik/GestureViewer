package viewer.VC;

import java.io.File;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.ToggleButton;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;
import viewer.Bridge;
import viewer.BridgeComponent;
import viewer.interfaces.IControllerViewState;
import viewer.interfaces.IViewState;

public class MediaTController extends BridgeComponent implements IControllerViewState {

    MediaPlayer mediaPlayer;
    Media media;

    @FXML
    private MediaView mediaView;
    @FXML
    private Label lblTime;
    @FXML
    private Slider sliderTime;
    @FXML
    private Label lblVolume;
    @FXML
    private Slider sliderVolume;
    @FXML
    private ToggleButton btnPause;

    private ChangeListener<Number> timeChangeListener;
    private ChangeListener<Number> volumeChangeListener;
    private ChangeListener<Duration> currentTimeChangeListener;
    private float currentVolume = 0.4f;

    @Override
    public void ready() {
        mediaView.setMediaPlayer(mediaPlayer);
        timeChangeListener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {

                Duration currentTime = mediaPlayer.getCurrentTime();
                int minutes = (int) Math.floor(currentTime.toMinutes());
                int seconds = (int) (currentTime.toSeconds() % 60.0);
                lblTime.setText(minutes + ":" + seconds);
                mediaPlayer.seek(mediaPlayer.getStopTime().multiply(sliderTime.getValue() / 100.0));
            }
        };

        currentTimeChangeListener = new ChangeListener<Duration>() {
            @Override
            public void changed(ObservableValue<? extends Duration> observable, Duration oldValue, Duration newValue) {
                Duration currentTime = mediaPlayer.getCurrentTime();
                Duration duration = mediaPlayer.getStopTime();
                int minutes = (int) Math.floor(currentTime.toMinutes());
                int seconds = (int) (currentTime.toSeconds() % 60.0);
                lblTime.setText(minutes + ":" + seconds);
                sliderTime.valueProperty().removeListener(timeChangeListener);
                sliderTime.setValue((currentTime.toMillis() / duration.toMillis()) * 100.0);
                sliderTime.valueProperty().addListener(timeChangeListener);
            }
        };

        volumeChangeListener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                int newVolume = (int) (newValue.floatValue() * 100.0f);
                lblVolume.setText("Hlasitost: " + newVolume);
                mediaPlayer.setVolume(sliderVolume.getValue());
                currentVolume = (float) sliderVolume.getValue();
            }
        };
        //najít vhodnou výšku
        mediaView.fitWidthProperty().bind(((MainWindowController) getBridgeReference().getController(Bridge.View.MainWindowP)).getStage().widthProperty());
        mediaView.fitHeightProperty().bind(((MainWindowController) getBridgeReference().getController(Bridge.View.MainWindowP)).getStage().heightProperty().subtract(195));
    }

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

        sliderVolume.valueProperty().removeListener(volumeChangeListener);
        sliderTime.valueProperty().removeListener(timeChangeListener);

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.currentTimeProperty().removeListener(currentTimeChangeListener);
        }

    }

    @Override
    public void saveState(Tab sourceTab) {
        if (mediaPlayer != null) {
            sliderVolume.valueProperty().removeListener(volumeChangeListener);
            mediaPlayer.currentTimeProperty().removeListener(currentTimeChangeListener);
            sliderTime.valueProperty().removeListener(timeChangeListener);

            mediaPlayer.pause();
        }
    }

    @Override
    public void loadState(Tab sourceTab) {

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.currentTimeProperty().removeListener(currentTimeChangeListener);
            sliderVolume.valueProperty().removeListener(volumeChangeListener);
            sliderTime.valueProperty().removeListener(timeChangeListener);
        }

        MainWindowController mainGUI = (MainWindowController) getBridgeReference().getController(Bridge.View.MainWindowP);
        VariantState state = mainGUI.getVariantTabState(sourceTab);
        try {
            media = new Media(state.filesInDirectory[state.fileIndex].toURI().toASCIIString());            
        }
        catch (MediaException e) {
            ((MainWindowController) getBridgeReference().getController(Bridge.View.MainWindowP)).setViewerStatus("Nepodařil se načíst multimediální soubor");
            mainGUI.setFilesCount(state.filesInDirectory, state.fileIndex, state.offset, true);
            mainGUI.closeSelectedTab();
            return;
        }
        mediaPlayer = new MediaPlayer(media);
        mainGUI.setFilesCount(state.filesInDirectory, state.fileIndex, state.offset, false);
        mediaPlayer.setVolume(currentVolume);
        sliderVolume.valueProperty().addListener(volumeChangeListener);
        mediaPlayer.currentTimeProperty().addListener(currentTimeChangeListener);
        sliderTime.valueProperty().addListener(timeChangeListener);

        mediaView.setMediaPlayer(mediaPlayer);
        mediaPlayer.play();
    }

    public void pauseMedium() {
        if (!btnPause.isSelected()) {
            mediaPlayer.play();
        }
        else {
            mediaPlayer.pause();
        }
    }

    @Override
    public int getNumberOfStates() {
        MainWindowController mainGUI = (MainWindowController) getBridgeReference().getController(Bridge.View.MainWindowP);
        return mainGUI.getNumberOfStates();
    }

}
