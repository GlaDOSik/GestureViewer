/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package viewer;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.opencv.core.Core;
;

/**

 @author Ludek
 */
public class Main extends Application {

    private Bridge bridge;
    
    @Override
    public void start(Stage stage) {
        try {
            
            //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            //odkomentovat pro export do jar
            loadLibrary();
            
            bridge = new Bridge(stage);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    private static void loadLibrary() {
        try {
            InputStream in = Main.class.getResourceAsStream("/opencv_java300.dll");
            File fileOut = File.createTempFile("lib", ".dll");
            OutputStream out = FileUtils.openOutputStream(fileOut);
            IOUtils.copy(in, out);
            in.close();
            out.close();
            System.load(fileOut.toString());
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to load opencv native library", e);
        }

    }
}
