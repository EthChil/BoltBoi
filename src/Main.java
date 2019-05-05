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

import static org.opencv.imgcodecs.Imgcodecs.*;


public class Main {

    public static SerialPort serialPort = new SerialPort("/dev/cu.usbmodem14101");

    //Bolt sizes in ints
    public static double[] sizing = new double[]{ 0.165, 0.295, 0.712, 0.985};
    public static double[] ARs = new double[]{2.218, 1.831, 4.440, 5.1};

    public static JLabel image = new JLabel();

    public static double fov = 71.6;
    public static double height = 5.2;

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

    public static double determineAR(double sizeX, double sizeY){
                    double ARy = sizeX / sizeY;
                    double ARx = sizeY / sizeX;

                    double AR = (ARy > ARx) ? ARy : ARx;
            return AR;
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

        //serialPort.openPort();


        //VARS
        double minSize = 2000;


        JFrame frame = new JFrame("output");
        JFrame feed = new JFrame("feed");

        //set properties of the frame
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 500);
        frame.add(image);

        VideoCapture cam = new VideoCapture(0);
        //cam.open("http://192.168.137.219:4747/mjpegfeed");

        Mat src = new Mat();
        src = Imgcodecs.imread("C:\\Users\\Brian-Laptop\\Documents\\GitHub\\BoltBoi\\src\\screw4.jpg");

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
            cam.read(src);
            while(src.cols() == 0)
                cam.read(src);


            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            Imgcodecs.imwrite("test,jpg", src);


            System.out.println(src.cols());

            //src = Imgcodecs.imread("C:\\Users\\Brian-Laptop\\Documents\\GitHub\\BoltBoi\\src\\screw4.jpg");


            //Mats needed for image processing
            Mat dst = new Mat();
            Mat out = new Mat();

            //Image filter values

            //resize the image to decrease the strain on the CPU
            //Imgproc.resize(src, src, new Size(src.cols() /4 , src.rows() / 4));


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
                    //System.out.println(determineBolt(calcSize(Imgproc.minAreaRect(dank).size.width, src.cols())) + " y = " + Double.toString(Imgproc.minAreaRect(dank).center.y));

                    //System.out.println("Size = " + Double.toString(calcSize(Imgproc.minAreaRect(dank).size.width, src.cols())) +
                    //        " AR = " + determineAR(Imgproc.minAreaRect(dank).size.width, Imgproc.minAreaRect(dank).size.height) +
                    //       " y = " + Double.toString(Imgproc.minAreaRect(dank).center.y));

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

            //sendMessage(Integer.toString((int)(furthestRightBolt + (furthestRight / 3.0))));
            //System.out.println(Integer.toString((int)(furthestRightBolt + (furthestRight / 3.0))));


            display(out);


//            wait 100ms to make the video viewable instead of quickly blinking past
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
