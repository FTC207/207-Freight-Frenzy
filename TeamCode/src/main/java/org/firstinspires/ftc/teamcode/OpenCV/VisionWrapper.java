package org.firstinspires.ftc.teamcode.OpenCV;

import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;
import org.openftc.easyopencv.OpenCvWebcam;

import java.util.ArrayList;
import java.util.List;

public class VisionWrapper {
    private OpenCvWebcam webcam;
    private GripPipeline grip;

    private static final int CAMERA_WIDTH = 320;

    public VisionWrapper(){ grip = new GripPipeline(); }

    public void init(HardwareMap hmap){ initVision(hmap); }

    private void initVision(HardwareMap hmap){
        // initialize the camera and pass in the pipeline
        int cameraMonitorViewId = hmap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hmap.appContext.getPackageName());
        webcam = OpenCvCameraFactory.getInstance().createWebcam(hmap.get(WebcamName.class, "pp"), cameraMonitorViewId);

        webcam.setPipeline(grip);
        webcam.setMillisecondsPermissionTimeout(2500); // Timeout for obtaining permission is configurable. Set before opening.

        // turn on the camera at given resolution
        webcam.openCameraDeviceAsync(new OpenCvCamera.AsyncCameraOpenListener() {
            @Override
            public void onOpened() {
                webcam.startStreaming(1280, 720, OpenCvCameraRotation.UPRIGHT);
            }

            @Override
            public void onError(int errorCode){ }
        });
    }

    // updates which level the camera sees the duck at
    private DetectionLevel updateDetermination(){
        // get the list of contours from the pipeline
        List<MatOfPoint> contours = new ArrayList<>(grip.findContoursOutput());

        // if there are no contours found, duck is out of camera frame meaning level 3
        if(contours.size() == 0){
            return DetectionLevel.LEVEL_THREE;
        }else{
            // find the average center of mass on the screen and if it is greater than 1/2 the
            //    width, it is level 2 and level 1 if less than half the width
            double xSum = 0;

            for (MatOfPoint point : contours) {
                Moments moments = Imgproc.moments(point);

                xSum += moments.get_m10() / moments.get_m00();
            }

            xSum /= contours.size();

            if (xSum > CAMERA_WIDTH/2) {
                return DetectionLevel.LEVEL_TWO;
            } else {
                return DetectionLevel.LEVEL_ONE;
            }
        }
    }

    // updates which level the camera sees the duck at
    private DetectionLevel updateDeterminationReverse(){
        // get the list of contours from the pipeline
        List<MatOfPoint> contours = new ArrayList<>(grip.findContoursOutput());

        // if there are no contours found, duck is out of camera frame meaning level 3
        if(contours.size() == 0){
            return DetectionLevel.LEVEL_ONE;
        }else{
            // find the average center of mass on the screen and if it is greater than 1/2 the
            //    width, it is level 2 and level 1 if less than half the width
            double xSum = 0;

            for (MatOfPoint point : contours) {
                Moments moments = Imgproc.moments(point);

                xSum += moments.get_m10() / moments.get_m00();
            }

            xSum /= contours.size();

            if (xSum > CAMERA_WIDTH/2) {
                return DetectionLevel.LEVEL_THREE;
            } else {
                return DetectionLevel.LEVEL_TWO;
            }
        }
    }

    public DetectionLevel currentDetermination(){ return this.updateDetermination(); }

    public DetectionLevel currentDeterminationReverse(){ return this.updateDeterminationReverse(); }

    // stops the camera
    public void stop(){ webcam.stopStreaming(); }

    public enum DetectionLevel{
        LEVEL_ONE("1 - Lowest level"), LEVEL_TWO("2 - Middle level"),
        LEVEL_THREE("3 - Top level"), UNKNOWN("Unknown");

        private final String level;

        DetectionLevel(String str) {
            this.level = str;
        }

        @Override
        public String toString() { return this.level; }
    }
}
