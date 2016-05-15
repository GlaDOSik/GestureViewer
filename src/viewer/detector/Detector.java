package viewer.detector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;
import org.opencv.videoio.VideoCapture;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import viewer.Bridge;
import viewer.VC.MainWindowController;

public class Detector implements IDetector {

    //předlohy pro testování podobnosti tvarů
    /*
    private final String[] TEMPLATE_NAMES = {"devilHorn.png", "fist.png", "indexFinger.png", "ok.png", "openHand.png", "pick.png"};  
    private final double[] similarity = new double[TEMPLATE_NAMES.length];*/
    //konstanty pro MOG2
    private final int BG_SUB_LEARNING_FRAMES = 30;
    private final int BG_SUB_HISTORY = 30;
    private final double BG_SUB_THRESHOLD = 11.0;

    //kernel pro dilataci a erozi
    private final Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(6, 6));
    //práh hloubky defektů
    private final int CONV_DEF_DEPTH_THRESHOLD = 4000;
    //vyfiltruje pryč defekty x pixelů nad spodním okrajem obrazu
    private final int CONV_DEF_EDGE_OFFSET = 30;
    //okolí pro redukci shluků bodů
    private final int FILTER_POINTS_AROUND = 20;
    //poloměr dlaně vynásobený fingerDetectionThreshold udává plochu, kde se body neakceptují jako prsty
    private float fingerDetectionThreshold = 1.4f;
    private final float FINGER_DET_THRESHOLD_MIN = fingerDetectionThreshold;
    //fingerDetectionThreshold se změní na FINGER_DET_THRESHOLD_MAX, pokud není natažen žádný prst - pěst
    //pomáhá tak lépe maskovat nežádoucí body
    private final float FINGER_DET_THRESHOLD_COUNT_0 = 2.0f;
    //stejné, jen pro jeden prst
    private final float FINGER_DET_THRESHOLD_COUNT_1 = 1.7f;

    private final VideoCapture vc;
    private final Helper helper;
    private ScheduledExecutorService executor;
    private boolean isDetectorActive = false;
    private boolean showDebugData = false;
    private boolean capture = false;

    private ImageView finnalImg;
    private ImageView rawImg;
    private ImageView foregroundImg;
    Bridge bridge;

    private final List<MatOfPoint> templateContours;

    public Detector(Bridge bridge) {
        templateContours = new ArrayList<>();
        helper = new Helper();
        vc = new VideoCapture();
        this.bridge = bridge;

        /// POROVNÁNÍ TVARŮ
        /*  for (int i = 0; i < similarity.length; i++) {
            similarity[i] = 0.0;
        }
        for (String TEMPLATE_NAMES1 : TEMPLATE_NAMES) {
            Mat ht = Imgcodecs.imread(TEMPLATE_NAMES1, 0);
            List<MatOfPoint> htcs = new ArrayList<>();
            Imgproc.findContours(ht, htcs, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
            MatOfPoint htc = helper.findBiggestContour(htcs, 5000);
            helper.approxPolyDPC(htc, APPROX_EPSILON);
            templateContours.add(htc);
        }*/
    }

    @Override
    public void start() {

        if (vc.open(0)) {
            isDetectorActive = true;
        }
        else {
            isDetectorActive = false;
        }

        BackgroundSubtractorMOG2 bgsubMOG2 = Video.createBackgroundSubtractorMOG2(BG_SUB_HISTORY, BG_SUB_THRESHOLD, false);

        // Jádro detektoru
        Runnable detector = new Runnable() {
            Mat sourceFrame = new Mat();
            Mat maskFrame = new Mat();
            int learningCounter = BG_SUB_LEARNING_FRAMES;
            Point palmCenter = new Point();

            // doplnit přeučení při změně pozadí
            @Override
            public void run() {
                vc.read(sourceFrame);
                if (learningCounter > 0) {

                    bgsubMOG2.apply(sourceFrame, maskFrame);
                    learningCounter--;
                    if (learningCounter == 1) {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                ((MainWindowController) bridge.getController(Bridge.View.MainWindowP)).setViewerStatus("Detektor je naučen, lze ovládat program kamerou.");
                            }
                        });
                    }
                }
                else {
                    bgsubMOG2.apply(sourceFrame, maskFrame, 0);
                }
                if (capture == true) {
                    Imgcodecs.imwrite("01Source.png", sourceFrame);
                    Imgcodecs.imwrite("02MaskRaw.png", maskFrame);
                }

                Imgproc.erode(maskFrame, maskFrame, kernel);
                Imgproc.dilate(maskFrame, maskFrame, kernel);

                if (capture == true) {
                    Imgcodecs.imwrite("03MaskAfterErode.png", maskFrame);
                }
                if (showDebugData) {
                    foregroundImg.setImage(helper.mat2Img(maskFrame));
                }

                List<MatOfPoint> contours = new ArrayList<>();
                Imgproc.findContours(maskFrame, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
                MatOfPoint contour = helper.findBiggestContour(contours, 20000);

                if (contour.rows() > 10) {

                    /// POROVNÁNÍ TVARŮ
                    /*double minSimilarity = 100;
                    int minSimilarityIndex = 0;
                    for (int i = 0; i < similarity.length; i++) {
                        similarity[i] = Imgproc.matchShapes(contour, templateContours.get(i), Imgproc.CV_CONTOURS_MATCH_I2, 0);
                        if (similarity[i] < minSimilarity) {
                            minSimilarity = similarity[i];
                            minSimilarityIndex = i;
                        }
                    }
                    System.out.println("Gesto je: " + TEMPLATE_NAMES[minSimilarityIndex]);*/
                    //vykreslení kontury
                    List<MatOfPoint> cstd = new ArrayList<>();
                    cstd.add(contour);
                    Imgproc.drawContours(sourceFrame, cstd, -1, new Scalar(12, 245, 0), 5);

                    //hull
                    MatOfInt hull = new MatOfInt();
                    Imgproc.convexHull(contour, hull);

                    List<MatOfPoint> hullMat = new ArrayList<>();
                    hullMat.add(helper.hullInts2Points(contour, hull));
                    Imgproc.drawContours(sourceFrame, hullMat, -1, new Scalar(255, 0, 0), 5);

                    //vykreslit natočený obdélník (BoundingBox) okolo kontury
                    /* MatOfPoint2f mop2f = new MatOfPoint2f();
                    contour.convertTo(mop2f, CvType.CV_32FC2);
                    RotatedRect rect = Imgproc.minAreaRect(mop2f);
                    helper.drawRotatedRect(rect, cFrame, new Scalar(0, 0, 255), 4);
                    Imgproc.circle(cFrame, rect.center, 5, new Scalar(255, 0, 255), 5);*/
                    //nalézt defekty
                    MatOfInt4 convDefects = new MatOfInt4();
                    Imgproc.convexityDefects(contour, hull, convDefects);
                    List<Point> defPointsFar = new ArrayList<>();

                    palmCenter.x = 0;
                    palmCenter.y = 0;
                    for (int j = 0; j < convDefects.rows(); j++) {
                        int convDefectsIndexes[] = new int[4];
                        //convDefectsIndexes[0] = (int) convDefects.get(j, 0)[0]; //začátek
                        //convDefectsIndexes[1] = (int) convDefects.get(j, 0)[1]; //konec
                        convDefectsIndexes[2] = (int) convDefects.get(j, 0)[2]; //defekt
                        convDefectsIndexes[3] = (int) convDefects.get(j, 0)[3]; //hloubka
                        //odstraníme defekty pod určitým prahem
                        if (convDefectsIndexes[3] > CONV_DEF_DEPTH_THRESHOLD) {
                            //Point pointStart = new Point(contour.get(convDefectsIndexes[0], 0)[0], contour.get(convDefectsIndexes[0], 0)[1]);
                            //Point pointEnd = new Point(contour.get(convDefectsIndexes[1], 0)[0], contour.get(convDefectsIndexes[1], 0)[1]);
                            Point pointFar = new Point(contour.get(convDefectsIndexes[2], 0)[0], contour.get(convDefectsIndexes[2], 0)[1]);
                            //odstraníme defekty pod u spodního okraje obrazu
                            if (pointFar.y <= sourceFrame.rows() - CONV_DEF_EDGE_OFFSET) {
                                defPointsFar.add(pointFar);
                                palmCenter.x += pointFar.x;
                                palmCenter.y += pointFar.y;
                                Imgproc.circle(sourceFrame, pointFar, 10, new Scalar(0, 255, 255), 4);
                            }
                        }
                    }

                    //filtrování prstů - redukce shluků
                    List<Point> tips = new ArrayList<>();
                    for (int i = 0; i < hullMat.get(0).rows() - 1; i++) {
                        Point nextP = new Point(hullMat.get(0).get(i + 1, 0));
                        //oblast okolo aktuálního bodu     
                        Rect nArea = new Rect((int) hullMat.get(0).get(i, 0)[0] - FILTER_POINTS_AROUND, (int) hullMat.get(0).get(i, 0)[1] - FILTER_POINTS_AROUND, FILTER_POINTS_AROUND * 2, FILTER_POINTS_AROUND * 2);
                        //pokud není soused blízko stávajícího bodu
                        if (!nextP.inside(nArea)) {
                            tips.add(nextP);
                            palmCenter.x += nextP.x;
                            palmCenter.y += nextP.y;
                            Imgproc.circle(sourceFrame, nextP, 5, new Scalar(0, 255, 255), 8);
                        }
                    }

                    //přibližná poloha dlaně
                    palmCenter.x /= defPointsFar.size() + tips.size();
                    palmCenter.y /= defPointsFar.size() + tips.size();

                    double palmRadius = 0.0;
                    for (int i = 0; i < defPointsFar.size(); i++) {
                        palmRadius += Math.sqrt(((defPointsFar.get(i).x - palmCenter.x) * (defPointsFar.get(i).x - palmCenter.x)) + ((defPointsFar.get(i).y - palmCenter.y) * (defPointsFar.get(i).y - palmCenter.y)));
                    }

                    palmRadius /= defPointsFar.size();
                    Imgproc.circle(sourceFrame, palmCenter, (int) palmRadius, new Scalar(255, 255, 255), 8);
                    Imgproc.circle(sourceFrame, palmCenter, (int) (palmRadius * fingerDetectionThreshold), new Scalar(255, 255, 255), 3);

                    //detekce počtu prstů
                    int fingerCount = 0;
                    //méně než 3 defekty je většinou pěst
                    if (defPointsFar.size() >= 3) {
                        for (int i = 0; i < tips.size(); i++) {
                            double tipDistance = Math.sqrt(((tips.get(i).x - palmCenter.x) * (tips.get(i).x - palmCenter.x)) + ((tips.get(i).y - palmCenter.y) * (tips.get(i).y - palmCenter.y)));
                            if (tipDistance > palmRadius * fingerDetectionThreshold && tips.get(i).y < palmCenter.y + palmRadius) {
                                fingerCount++;
                            }
                        }
                    }

                    
                    if (fingerCount == 0) {
                        fingerDetectionThreshold = FINGER_DET_THRESHOLD_COUNT_0;
                    }
                    else if (fingerCount == 1) {
                        fingerDetectionThreshold = FINGER_DET_THRESHOLD_COUNT_1;
                    }
                    else{
                        fingerDetectionThreshold = FINGER_DET_THRESHOLD_MIN;
                    }
                    DetectorController.setFingerCount(Math.min(fingerCount, 5));
                    DetectorController.setPalmCenter(palmCenter.x, palmCenter.y);

                }
                if (showDebugData) {
                    finnalImg.setImage(helper.mat2Img(sourceFrame));
                }

            }
        };

        //ošetřit timer
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(detector, 0, 33, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (isDetectorActive) {
            isDetectorActive = false;
            vc.release();
            executor.shutdown();
            try {
                executor.awaitTermination(33, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            executor = null;
        }

    }

    @Override
    public boolean isDetectorActive() {
        return isDetectorActive;
    }


    public boolean showDebugData() {
        return showDebugData;
    }

    public void setShowDebugData(boolean showDebugData) {
        this.showDebugData = showDebugData;
    }

    public void setImageView(ImageView view1, ImageView view2) {
        finnalImg = view1;
        foregroundImg = view2;
    }

}
