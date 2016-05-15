package viewer.detector;

import com.sun.glass.ui.Robot;
import javafx.application.Platform;
import javafx.scene.control.Label;
import viewer.Bridge;
import viewer.VC.MainWindowController;

public class DetectorController {

    public DetectorController(Bridge bridgeReference) {
        this.bridgeReference = bridgeReference;
        robot = com.sun.glass.ui.Application.GetApplication().createRobot();
    }
    private static final int BUFFER_SIZE = 8;
    private static final float FINGER_COUNT_THRESHOLD = 0.5f;
    private static final int threshold = (int) (BUFFER_SIZE * FINGER_COUNT_THRESHOLD);

    private static int palmCenterBufferIndex = 0;
    private static double[] palmCenterXBuffer = new double[BUFFER_SIZE];
    private static double[] palmCenterYBuffer = new double[BUFFER_SIZE];
    private static double prevAveragePalmCenterX = 0;
    private static double prevAveragePalmCenterY = 0;

    private static int fingerCountBufferIndex = 0;
    private static int[] fingerCountBuffer = new int[BUFFER_SIZE];
    private static int[] countRate = new int[]{0, 0, 0, 0, 0, 0};

    private static int gesture;
    private static int prevGesture = -1;
    private static double deltaX = 0.0;
    private static double deltaY = 0.0;

    private static Bridge bridgeReference;
    private static Robot robot;
    private static Label viewerStatus;

    public static void setFingerCount(int fingerCount) {
        //počet prstů do posuvného bufferu
        fingerCountBufferIndex++;
        if (fingerCountBufferIndex == BUFFER_SIZE) {
            fingerCountBufferIndex = 0;
        }
        fingerCountBuffer[fingerCountBufferIndex] = fingerCount;

        //zjištění četností, odfiltrování šumu
        for (int i = 0; i < 6; i++) {
            countRate[i] = 0;
        }
        for (int i = 0; i < BUFFER_SIZE; i++) {
            countRate[fingerCountBuffer[i]]++;
        }
        //pokud je četnost větší nebo se rovná prahu, gesto je přijato
        gesture = -1;
        for (int i = 0; i < 6; i++) {
            if (countRate[i] >= threshold) {
                gesture = i;
            }
        }

        //TODO - dále analyzovat další odvozená gesta ??
        //pokud prst nebyl natažen, ale teď je - klik
        if (gesture == 1 && prevGesture != 1) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    System.out.println("press");
                    robot.mousePress(1);
                }
            });
        }
        //pokud prst byl natažen, ale teď není - puštění
        else if (gesture != 1 && prevGesture == 1) {
            System.out.println("release");
            robot.mouseRelease(1);
        }
        else if (gesture == 2) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    System.out.println("Pravý klik");
                    
                    robot.mousePress(2);
                    robot.mouseRelease(2);
                }
            });
        }
        else if (gesture == 3) {

            if (deltaX > 40) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        ((MainWindowController) bridgeReference.getController(Bridge.View.MainWindowP)).prevFile();
                    }
                });
            }
            else if (deltaX < -40) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        ((MainWindowController) bridgeReference.getController(Bridge.View.MainWindowP)).nextFile();
                    }
                });
            }

        }
        System.out.println("gesto 3:   " + deltaX);
        prevGesture = gesture;
    }

    public static void setPalmCenter(double palmCenterX, double palmCenterY) {
        palmCenterBufferIndex++;
        if (palmCenterBufferIndex == BUFFER_SIZE) {
            palmCenterBufferIndex = 0;
        }
        palmCenterXBuffer[palmCenterBufferIndex] = palmCenterX;
        palmCenterYBuffer[palmCenterBufferIndex] = palmCenterY;

        double averageCenterX = 0;
        double averageCenterY = 0;
        for (int i = 0; i < BUFFER_SIZE; i++) {
            averageCenterX += palmCenterXBuffer[palmCenterBufferIndex];
            averageCenterY += palmCenterYBuffer[palmCenterBufferIndex];
        }
        averageCenterX /= BUFFER_SIZE;
        averageCenterY /= BUFFER_SIZE;

        deltaX = (averageCenterX - prevAveragePalmCenterX);
        deltaY = (averageCenterY - prevAveragePalmCenterY);
        prevAveragePalmCenterX = averageCenterX;
        prevAveragePalmCenterY = averageCenterY;
        if (gesture != 0) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    //System.out.println(robot.getMouseX() + robot.getMouseY());
                    robot.mouseMove(robot.getMouseX() - (int) deltaX, robot.getMouseY() + (int) deltaY);
                }
            });
        }
        else if (prevGesture != 0 && gesture == 0) {
            prevAveragePalmCenterX = 0.0;
            prevAveragePalmCenterY = 0.0;
            for (int i = 0; i < BUFFER_SIZE; i++) {
                palmCenterXBuffer[i] = 0.0f;
                palmCenterYBuffer[i] = 0.0f;
            }
        }
    }

}
