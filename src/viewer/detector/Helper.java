package viewer.detector;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javafx.scene.image.Image;
import org.opencv.core.MatOfPoint2f;

/*
 * Třída pomocných funkcí
 */
public class Helper {
    
    private MatOfPoint empty = new MatOfPoint();
    private int maxAreaI = -1;

    public Image mat2Img(Mat frame) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", frame, buffer);
        return new Image(new ByteArrayInputStream(buffer.toArray()));    
    }
    
   
    public MatOfPoint findBiggestContour(List<MatOfPoint> contours, double thresholdArea) {
        double maxArea = -1;
        maxAreaI = -1;

        //kontury
        for (int i = 0; i < contours.size(); i++) {
            //oblast pro každou konturu
            double contourArea = Imgproc.contourArea(contours.get(i));
            //najde největší oblast
            if (contourArea > maxArea) {
                maxArea = contourArea;
                maxAreaI = i;
            }
        }
        if (maxArea < thresholdArea) {
            return empty;
        }
        MatOfPoint tempMat = contours.get(maxAreaI);
        return tempMat;
    }
    
 

    public MatOfPoint hullInts2Points(MatOfPoint contour, MatOfInt hull) {
        Point[] hullPoints = new Point[hull.rows()];
        for (int i = 0; i < hull.rows(); i++) {
            int index = (int) hull.get(i, 0)[0];
            hullPoints[i] = new Point(contour.get(index, 0)[0], contour.get(index, 0)[1]);
        }
        return new MatOfPoint(hullPoints);
    }

    public void drawRotatedRect(RotatedRect rect, Mat sourceMat, Scalar color, int thickness) {
        Point[] points = new Point[4];
        rect.points(points);
        Imgproc.line(sourceMat, points[0], points[1], color, thickness);
        Imgproc.line(sourceMat, points[1], points[2], color, thickness);
        Imgproc.line(sourceMat, points[2], points[3], color, thickness);
        Imgproc.line(sourceMat, points[3], points[0], color, thickness);
    }

    public void approxPolyDPC(MatOfPoint contour, double epsilon) {
        MatOfPoint2f mop2f = new MatOfPoint2f();
        contour.convertTo(mop2f, CvType.CV_32FC2);
        Imgproc.approxPolyDP(mop2f, mop2f, epsilon, true);
        mop2f.convertTo(contour, contour.type());
    }

}
