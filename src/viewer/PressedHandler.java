package viewer;

import javafx.event.EventHandler;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;



public class PressedHandler implements EventHandler<MouseEvent> {

    public PressedHandler(ScrollPane scrollPane){
        this.scrollPane = scrollPane;
    }

    private ScrollPane scrollPane; 
    
    private static double clickPositionX;
    private static double clickPositionY;

    @Override
    public void handle(MouseEvent event){
        clickPositionX = (int) event.getScreenX();
        clickPositionY = (int) event.getScreenY();

        event.consume();
    }

    public static void setClickPositionX(double clickPositionX){
        PressedHandler.clickPositionX = clickPositionX;
    }

    public static void setClickPositionY(double clickPositionY){
        PressedHandler.clickPositionY = clickPositionY;
    }

    public static double getClickPosX(){
        return clickPositionX;
    }

    public static double getClickPosY(){
        return clickPositionY;
    }   

}
