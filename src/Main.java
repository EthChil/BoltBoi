import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.util.ArrayList;

public class Main {

    public static JLabel image = new JLabel();

    //static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }
    static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    public static void display(Mat gang){
        //creates a buffered image to display the output on
        BufferedImage img = new BufferedImage(gang.width(), gang.height(), BufferedImage.TYPE_3BYTE_BGR);
        WritableRaster raster = img.getRaster();
        DataBufferByte dataBuffer = (DataBufferByte) raster.getDataBuffer();
        byte[] data = dataBuffer.getData();
        gang.get(0, 0, data);

        //display the output on the JLabel
        image.setIcon(new ImageIcon(img));
        image.repaint();
    }

    public static void main(String[] args) throws Exception {

        //System.load("/usr/local/Cellar/opencv/4.1.0_1/share/java/libopencv_java410.dylib");


        JFrame frame = new JFrame("output");
        JFrame feed = new JFrame("feed");

        //set properties of the frame
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 500);
        frame.add(image);

        VideoCapture cam = new VideoCapture(0);

        //cam.open(0);

        Mat src = new Mat();

        //loops forever
        while (true) {

            //read a frame from the video in
            cam.read(src);

            //Mats needed for image processing
            Mat dst = new Mat();
            Mat out = new Mat();

            //Image filter values
            int VSub = 100;
            int VAdd = 255;

            int SSub = 100;
            int SAdd = 255;

            int HSub = 100;
            int HAdd = 255;

            //resize the image to decrease the strain on the CPU
            Imgproc.resize(src, src, new Size(src.cols() / 2, src.rows() / 2));

            //make the high and low filter values
            Scalar hsvLow = new Scalar(HSub, SSub, VSub);
            Scalar hsvHigh = new Scalar(HAdd, SAdd, VAdd);

            //filter the image by Hue Saturation and Value then save the mask into dst
            Core.inRange(src, hsvLow, hsvHigh, dst);

            //contour array lists
            ArrayList<MatOfPoint> contours1 = new ArrayList<MatOfPoint>();
            Mat hierarchy1 = new Mat();

            ArrayList<MatOfPoint> contours2 = new ArrayList<MatOfPoint>();
            Mat hierarchy2 = new Mat();

            //hierarchy Mat
            Mat hierarchy = new Mat();

            //create contours around shapes in the mask
            Imgproc.findContours(dst, contours1, hierarchy1, 2, 1);
            Imgproc.findContours(dst, contours2, hierarchy2, 2, 1);

            //copy the original image to the output Mat that the contours are overlayed to
            src.copyTo(out);

//            //Sort the contours by rough shape removing those that fall under a certain threshold
//            for (int i = 0; i < contours2.size(); i++) {
//                double val = Imgproc.matchShapes(contours2.get(i), contours1.get(0), 1, 0.0);
//
//                if (val > 600)
//                    contours2.remove(i);
//            }

//            //variables for size based sorting
//            double largest = 0;
//            int index = -1;
//
//            //Sort out the biggest contours
//            for (int i = 0; i < contours2.size(); i++) {
//                double size = Imgproc.boundingRect(contours2.get(i)).width;
//
//                if (size > largest) {
//                    index = i;
//                    largest = size;
//                }
//
//            }

            //Sort contours based on aspect ratio

            for (int i = 0; i < contours2.size(); i++) {
                double sizeX = Imgproc.boundingRect(contours2.get(i)).width;
                double sizeY = Imgproc.boundingRect(contours2.get(i)).width;

                if (size > largest) {
                    index = i;
                    largest = size;
                }

            }

            //draw the contour onto the output image
            Imgproc.drawContours(out, contours2, index, new Scalar(255, 0, 255), 2, 8, hierarchy, 0, new Point());

            //Checks to ensure there is a contour
            if (index > -1) {

                //variables for the different dimensions for the box
                int x = Imgproc.boundingRect(contours2.get(index)).x;
                int y = Imgproc.boundingRect(contours2.get(index)).y;

                int width = Imgproc.boundingRect(contours2.get(index)).width;
                int height = Imgproc.boundingRect(contours2.get(index)).height;

                //prints the NavX turn angle on the RAS Pi this goes through the GPIO pins
                System.out.println("angle to turn = " + (0.09375) * ((x + (width / 2)) - (out.cols() / 2)));

                //places a rectangle over the output image
                Imgproc.rectangle(out, new Point(x, y), new Point(x + width, y + height), new Scalar(255, 128, 0), 2, 2,
                        0);

            }

            display(out);

            //wait 100ms to make the video viewable instead of quickly blinking past
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
