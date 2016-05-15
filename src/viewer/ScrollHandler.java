package viewer;

import javafx.event.EventHandler;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;


public class ScrollHandler implements EventHandler<ScrollEvent> {

    public ScrollHandler(ImageView iw, ScrollPane scrollPane, StackPane stackPane) {
        this.stackPane = stackPane;
        this.scrollPane = scrollPane;
        this.iw = iw;
    }

    private StackPane stackPane;
    private ScrollPane scrollPane;
    private ImageView iw;

    @Override
    public void handle(ScrollEvent event) {
        if (event.getDeltaY() < 0) {
            if ((int) stackPane.getScaleX() < 8) {
                stackPane.setScaleX(stackPane.getScaleX() + 0.05);
                stackPane.setScaleY(stackPane.getScaleY() + 0.05);

                scrollPane.setHvalue(event.getX() / scrollPane.getMinWidth());
                scrollPane.setVvalue(event.getY() / scrollPane.getMinHeight());
            }
        }
        else if (iw.getScaleX() >= 0.6) {
            stackPane.setScaleX(stackPane.getScaleX() - 0.05);
            stackPane.setScaleY(stackPane.getScaleY() - 0.05);

            scrollPane.setHvalue(event.getX() / scrollPane.getMinWidth());
            scrollPane.setVvalue(event.getY() / scrollPane.getMinHeight());
        }
        event.consume();
    }

}
