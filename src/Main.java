import jssc.SerialPortException;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import java.io.*;
import java.util.*;
import jssc.SerialPort;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;

import static org.opencv.imgcodecs.Imgcodecs.imdecode;
import static org.opencv.imgcodecs.Imgcodecs.imread;



public class Main {

    public static SerialPort serialPort = new SerialPort("/dev/cu.usbmodem14101");

    //Bolt sizes in ints
    public static int[] sizing = new int[]{ 1,2,3,4,5,6};
    public static double[] ARs = new double[]{1.0,2.0,3.0,4.0,5.0,6.0};

    public static JLabel image = new JLabel();

    public static double fov = 45;
    public static double height = 5;

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

    public static double calcSize(double pix, double w){
        double bottom = (Math.tan(Math.toRadians(fov/2)) * height)*2;

        //System.out.println(bottom);

        double inchPerPx = bottom/w;

        return pix * inchPerPx;
    }

    public static int determineBolt(double size){
        double bestScore = 100000;
        int bestInd = 0;

        for(int i = 0; i < sizing.length; i++){
            if(Math.abs(sizing[i] - size) < bestScore){
                bestScore = Math.abs(sizing[i] - size);
                bestInd = i;
            }
        }

        return (bestInd+1) * 1000;
    }

    public static void sendMessage(String msg){

        try {
            //Open port
            //We expose the settings. You can also use this line - serialPort.setParams(9600, 8, 1, 0);
            serialPort.setParams(SerialPort.BAUDRATE_115200,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);
            //Writes data to port
            serialPort.writeBytes(msg.getBytes());

        }
        catch (SerialPortException ex) {
            System.out.println(ex);
        }
    }

    public static void main(String[] args) throws Exception {

        serialPort.openPort();

        //System.load("/usr/local/Cellar/opencv/4.1.0_1/share/java/libopencv_java410.dylib");

        //VARS
        double minSize = 200;


        JFrame frame = new JFrame("output");
        JFrame feed = new JFrame("feed");

        //set properties of the frame
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 500);
        frame.add(image);

        //VideoCapture cam = new VideoCapture();
        //cam.open(0);

        Mat src = new Mat();
        src = Imgcodecs.imread("/Users/Kratos/Documents/BoltBoi/src/screw3.jpg");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        int VSub, VAdd, SSub, SAdd, HSub, HAdd;

        VSub = 0;
        VAdd = 150;
        SSub = 0;
        SAdd = 90;
        HSub = 0;
        HAdd = 110;

        String text = "";

        //loops forever
        while (true) {

            if(reader.ready()) {
                text = reader.readLine();
            }

            String[] splited = text.split("\\s+");


            if (splited.length >= 2){
                //VSub = Integer.parseInt(splited[0]);
                //VAdd = Integer.parseInt(splited[1]);

                //SSub = Integer.parseInt(splited[0]);
                //SAdd = Integer.parseInt(splited[1]);

                //HSub = Integer.parseInt(splited[0]);
                //HAdd = Integer.parseInt(splited[1]);
            }

            //read a frame from the video in
            //cam.read(src);

            src = Imgcodecs.imread("/Users/Kratos/Documents/BoltBoi/src/screw3.jpg");


            //Mats needed for image processing
            Mat dst = new Mat();
            Mat out = new Mat();

            //Image filter values


            //resize the image to decrease the strain on the CPU
            Imgproc.resize(src, src, new Size(src.cols() /4 , src.rows() / 4));

            //make the high and low filter values
            Scalar hsvLow = new Scalar(HSub, SSub, VSub);
            Scalar hsvHigh = new Scalar(HAdd, SAdd, VAdd);

            //filter the image by Hue Saturation and Value then save the mask into dst
            Core.inRange(src, hsvLow, hsvHigh, dst);

            //display(dst);

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

            //Sort contours based on aspect ratio
//            for (int i = 0; i < contours2.size(); i++) {
//                    double sizeX = Imgproc.minAreaRect(new Mat(contours2.get(i))).size.width;
//                    double sizeY = Imgproc.minAreaRect(new Mat(contours2.get(i))).size.height;
//                    double ARy = sizeX / sizeY;
//                    double ARx = sizeY / sizeX;
//
//
//                    double AR = (ARy > ARx) ? ARy : ARx;
//
//                    if (AR > maxAR) {
//                        contours2.remove(i);
//                    }
//            }




//            //Sort the contours by rough shape removing those that fall under a certain threshold
//            for (int i = 0; i < contours2.size(); i++) {
//                double val = Imgproc.matchShapes(contours2.get(i), contours1.get(0), 1, 0.0);
//
//                if (val > 600)
//                    contours2.remove(i);
//            }

            //variables for size based sorting
            double largest = 0;
            int index = -1;

            //Sort out the biggest contours
            for (int i = 0; i < contours2.size(); i++) {
                double size = Imgproc.boundingRect(contours2.get(i)).area();
                //System.out.println(size);

                if (size < 500) {
                    contours2.remove(i);
                }
            }


            for (int i = 0; i < contours2.size(); i++) {
                Imgproc.drawContours(out, contours2, i, new Scalar(255, 0, 255), 1, 8, hierarchy, 0, new Point());
            }


            double furthestRight = 0;
            double furthestRightY = 0;
            int furthestRightBolt = 0;
            int indexFurthestRight = 0;

            for (int i = 0; i < contours2.size(); i++) {
                MatOfPoint2f dank = new MatOfPoint2f();
                contours2.get(i).convertTo(dank, CvType.CV_32F);

                if(Imgproc.minAreaRect(dank).size.area() > minSize) {
                    Point[] vertices = new Point[4];
                    Imgproc.minAreaRect(dank).points(vertices);
                    for (int j = 0; j < 4; j++) {
                        Imgproc.line(out, vertices[j], vertices[(j + 1) % 4], new Scalar(0, 255, 0));
                    }

                    if(Imgproc.minAreaRect(dank).center.x > furthestRight){
                        furthestRight = Imgproc.minAreaRect(dank).center.x;
                        furthestRightBolt = determineBolt(calcSize(Imgproc.minAreaRect(dank).size.width, src.cols()));
                        furthestRightY = Imgproc.minAreaRect(dank).center.y;
                        indexFurthestRight = i;
                    }
                }
            }

            //System.out.println("xPosFarRight = " + Double.toString(furthestRight));
            //System.out.println("yPosFarRight = " + Double.toString(furthestRightY));
            //System.out.println("furthestRightBoltSize = " + Double.toString(furthestRightBolt) + (Double.toString(furthestRight/3)));

            sendMessage(Integer.toString((int)(furthestRightBolt + (furthestRight / 3.0))));
            System.out.println(Integer.toString((int)(furthestRightBolt + (furthestRight / 3.0))));





//            //draw the contour onto the output image
//            Imgproc.drawContours(out, contours2, index, new Scalar(255, 0, 255), 2, 8, hierarchy, 0, new Point());
//
//            //Checks to ensure there is a contour
//            if (index > -1) {
//
//                //variables for the different dimensions for the box
//                int x = Imgproc.minAreaRect(contours2.get(index)).x;
//                int y = Imgproc.minAreaRect(contours2.get(index)).y;
//
//                int width = Imgproc.minAreaRect(contours2.get(index)).width;
//                int height = Imgproc.minAreaRect(contours2.get(index)).height;
//
//                //prints the NavX turn angle on the RAS Pi this goes through the GPIO pins
//                System.out.println("angle to turn = " + (0.09375) * ((x + (width / 2)) - (out.cols() / 2)));
//
//                //places a rectangle over the output image
//                Imgproc.rectangle(out, new Point(x, y), new Point(x + width, y + height), new Scalar(255, 128, 0), 2, 2,
//                        0);
//
//            }


            display(out);




//            wait 100ms to make the video viewable instead of quickly blinking past
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
