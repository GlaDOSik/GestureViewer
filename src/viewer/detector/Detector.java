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

    private int renderGesture = 6;
    private final Point textPos = new Point(20, 30);
    //předlohy pro testování podobnosti tvarů
    /*
    private final String[] TEMPLATE_NAMES = {"devilHorn.png", "fist.png", "indexFinger.png", "ok.png", "openHand.png", "pick.png"};  
    private final double[] similarity = new double[TEMPLATE_NAMES.length];*/
    //konstanty pro MOG2
    private final int BG_SUB_LEARNING_FRAMES = 50;
    private final int BG_SUB_HISTORY = 50;
    private final double BG_SUB_THRESHOLD = 4.0;
    //pokud je (RELEARN_THRESHOLD * 100) % pixelů v obrazu detekováno jako kontura, nejspíš se změnilisvětelné podmínky
    // nebo se posunula kamera, detektor se pak sám přeučí
    private final float RELEARN_THRESHOLD = 0.55f;

    //kernel pro dilataci a erozi
    private final Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(8, 8));
    //práh velikosti největší kontury
    private final int CONTOUR_AREA = 15000;
    
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
    private final float FINGER_DET_THRESHOLD_COUNT_0 = 1.9f;
    //stejné, jen pro jeden prst
    private final float FINGER_DET_THRESHOLD_COUNT_1 = 1.6f;
    //pokud se natáhne jeden prst, detekovaný střed dlaně klesne, tato hodnota to kompenzuje
    //private final float ONE_FINGER_Y_OFFSET = -1.3f;

    private final VideoCapture vc;
    private final BackgroundSubtractorMOG2 bgsubMOG2;
    private final Helper helper;
    private ScheduledExecutorService executor;
    private boolean isDetectorActive = false;
    private boolean showDebugData = false;
    private boolean capture = false;

    //výstup z kamery
    private final Mat sourceFrame;
    //výstup MOG2
    private final Mat maskFrame;
    //seznam kontur
    private List<MatOfPoint> contours;
    //seznam s jednou dominantní konturou pro vykreslení
    List<MatOfPoint> cstd;
    //indexy konvexního polygonu
    private MatOfInt hull;

    private ImageView finnalImg;
    private ImageView foregroundImg;
    Bridge bridgeReference;

    private final List<MatOfPoint> templateContours;
    
    private final Scalar CONTOUR_COLOR = new Scalar(12, 245, 0);

    public Detector(Bridge bridge) {
        templateContours = new ArrayList<>();
        helper = new Helper();
        vc = new VideoCapture();
        this.bridgeReference = bridge;
        bgsubMOG2 = Video.createBackgroundSubtractorMOG2(BG_SUB_HISTORY, BG_SUB_THRESHOLD, false);
        sourceFrame = new Mat();
        maskFrame = new Mat();                
        contours = new ArrayList<>();
        cstd = new ArrayList<>();
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
        } else {
            isDetectorActive = false;
        }

        //tady se vytvářel lokální MOG2
        // Jádro detektoru
        Runnable detector = new Runnable() {
            int learningCounter = BG_SUB_LEARNING_FRAMES;
            Point palmCenter = new Point();

            @Override
            public void run() {
                vc.read(sourceFrame);
                if (learningCounter > 0) {
                    if (learningCounter == BG_SUB_LEARNING_FRAMES) {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                ((MainWindowController) bridgeReference.getController(Bridge.View.MainWindowP)).setViewerStatus("Přesuňte se prosím mimo záběr kamery. Učím se pozadí.");
                            }
                        });
                    } else if (learningCounter == 1) {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                ((MainWindowController) bridgeReference.getController(Bridge.View.MainWindowP)).setViewerStatus("Detektor je naučen, lze ovládat program kamerou.");
                            }
                        });
                    }
                    bgsubMOG2.apply(sourceFrame, maskFrame);
                    learningCounter--;
                    return;
                } else {
                    bgsubMOG2.apply(sourceFrame, maskFrame, 0);
                }
                
                if (capture == true) {
                    Imgcodecs.imwrite("01Source.png", sourceFrame);
                    Imgcodecs.imwrite("02MaskRaw.png", maskFrame);
                }
                
                Imgproc.erode(maskFrame, maskFrame, kernel);
                Imgproc.dilate(maskFrame, maskFrame, kernel);

                //přeučení při změně světelných podmínek
                if ((float) Core.countNonZero(maskFrame) / (float) (maskFrame.rows() * maskFrame.cols()) > RELEARN_THRESHOLD) {
                    learningCounter = BG_SUB_LEARNING_FRAMES;
                    return;
                }

                if (capture == true) {
                    Imgcodecs.imwrite("03MaskAfterMO.png", maskFrame);
                }
                if (showDebugData) {
                    foregroundImg.setImage(helper.mat2Img(maskFrame));
                }

                //nalezení kontur, vybrání té největší
                contours.clear();
                Imgproc.findContours(maskFrame, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
                MatOfPoint contour = helper.findBiggestContour(contours, CONTOUR_AREA);
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
                    cstd.clear();
                    cstd.add(contour);
                    Imgproc.drawContours(sourceFrame, cstd, -1, CONTOUR_COLOR, 5);

                    //nalezení konvexního polygonu obepínajícího konturu
                    hull = new MatOfInt();
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
                        int convDefectsIndexes[] = new int[2];
                        convDefectsIndexes[0] = (int) convDefects.get(j, 0)[2]; //defekt
                        convDefectsIndexes[1] = (int) convDefects.get(j, 0)[3]; //hloubka
                        //odstraníme defekty pod určitým prahem
                        if (convDefectsIndexes[1] > CONV_DEF_DEPTH_THRESHOLD) {
                            Point pointFar = new Point(contour.get(convDefectsIndexes[0], 0)[0], contour.get(convDefectsIndexes[0], 0)[1]);
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

                    //detekce počtu prstů
                    int fingerCount = 0;
                    //méně než 3 defekty je většinou pěst
                    if (defPointsFar.size() >= 3) {
                        for (int i = 0; i < tips.size(); i++) {
                            Point p = tips.get(i);
                            double tipDistance = Math.sqrt(((p.x - palmCenter.x) * (p.x - palmCenter.x)) + ((p.y - palmCenter.y) * (p.y - palmCenter.y)));
                            if (tipDistance > palmRadius * fingerDetectionThreshold && p.y < palmCenter.y + palmRadius) {
                                fingerCount++;
                                Imgproc.circle(sourceFrame, p, 5, new Scalar(0, 0, 255), 8);
                            }
                        }
                    }
       
                    if (fingerCount == 0) {
                        fingerDetectionThreshold = FINGER_DET_THRESHOLD_COUNT_0;
                    } else if (fingerCount == 1) {
                        fingerDetectionThreshold = FINGER_DET_THRESHOLD_COUNT_1;
                        //mouseOffsetY = ONE_FINGER_Y_OFFSET;
                    } else {
                        fingerDetectionThreshold = FINGER_DET_THRESHOLD_MIN;
                    }
                              
                    Imgproc.circle(sourceFrame, palmCenter, (int) palmRadius, new Scalar(255, 255, 255), 8);
                    Imgproc.circle(sourceFrame, palmCenter, (int) (palmRadius * fingerDetectionThreshold), new Scalar(255, 255, 255), 3);

                    DetectorController.setFingerCount(Math.min(fingerCount, 5));
                    DetectorController.setPalmCenter(palmCenter.x, palmCenter.y);

                    Imgproc.putText(sourceFrame, String.format("%d", renderGesture), textPos, 0, 1, new Scalar(255, 255, 255), 3);

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
            } catch (InterruptedException e) {
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

    public void setRenderIcon(int icon) {
        renderGesture = icon;
    }

}
