/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package viewer;

import javafx.event.EventHandler;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

/**

 @author Ludek
 */
public class DragHandler implements EventHandler<MouseEvent> {

    public DragHandler(ScrollPane scrollPane){
        this.scrollPane = scrollPane;
    }

    private ScrollPane scrollPane;

    @Override
    public void handle(MouseEvent event){
        if (event.getButton() == MouseButton.PRIMARY){
            double deltaX = (PressedHandler.getClickPosX() - event.getScreenX()) / scrollPane.getMinWidth();
            double deltaY = (PressedHandler.getClickPosY() - event.getScreenY()) / scrollPane.getMinHeight();
            scrollPane.setHvalue(scrollPane.getHvalue() + deltaX*0.02);
            scrollPane.setVvalue(scrollPane.getVvalue() + deltaY*0.02);
            PressedHandler.setClickPositionX(event.getScreenX());
            PressedHandler.setClickPositionY(event.getScreenY());
        }
        event.consume();
    }
}
