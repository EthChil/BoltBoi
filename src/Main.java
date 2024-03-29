import jssc.SerialPortException;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.awt.*;
import java.awt.event.InputEvent;
import java.io.*;
import java.util.*;
import jssc.SerialPort;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;

import java.io.File;

import static org.opencv.imgcodecs.Imgcodecs.*;


public class Main {
    public static void takeScreenShot(String screenShotName) throws AWTException, IOException {

        Robot robot = new Robot();
        String path = "C:\\Users\\Brian-Laptop\\Downloads";
        String extension  = "jpg";
        String fileName = path +"\\"+ screenShotName +"."+ extension;

        Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        BufferedImage screenFullImage = robot.createScreenCapture(screenRect);
        ImageIO.write(screenFullImage, extension , new File(fileName));

    }


    public static void click(int x, int y, String s) throws AWTException{
        Robot bot = new Robot();

//        bot.mouseMove(x, y);
//
//
//        bot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
//        bot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

        bot.keyPress(KeyEvent.VK_CONTROL);
        bot.keyPress(KeyEvent.VK_L);
// CTRL+Z is now pressed (receiving application should see a "key down" event.)
        bot.keyRelease(KeyEvent.VK_L);
        bot.keyRelease(KeyEvent.VK_CONTROL);
//        leftClick();
        bot.delay(100);

        bot.keyPress(KeyEvent.VK_CONTROL);
        bot.keyPress(KeyEvent.VK_V);
// CTRL+Z is now pressed (receiving application should see a "key down" event.)
        bot.keyRelease(KeyEvent.VK_V);
        bot.keyRelease(KeyEvent.VK_CONTROL);

        bot.delay(200);

        bot.keyPress(KeyEvent.VK_ENTER);
        bot.keyRelease(KeyEvent.VK_ENTER);

        bot.delay(400);

        byte[] bytes = s.getBytes();
        for (byte b : bytes)
        {
            int code = b;
            // keycode only handles [A-Z] (which is ASCII decimal [65-90])
            if (code > 96 && code < 123) code = code - 32;
            bot.delay(40);
            bot.keyPress(code);
            bot.keyRelease(code);
        }
        bot.delay(250);

        bot.keyPress(KeyEvent.VK_ENTER);
        bot.keyRelease(KeyEvent.VK_ENTER);
    }



    public static SerialPort serialPort = new SerialPort("COM21");

    //Bolt sizes in ints
    public static double[] sizing = new double[]{ 0.191, 0.311, 0.458, 0.5654};
    public static double[] ARs = new double[]{2.218, 1.831, 4.440, 5.1};

    public static JLabel image = new JLabel();

    public static double fov = 45;
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

        serialPort.openPort();


        //VARS
        double minSize = 500;
        double maxSize = 5000;


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
        src = Imgcodecs.imread("C:\\Users\\Brian-Laptop\\Downloads\\jamhacks.jpg");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        int VSub, VAdd, SSub, SAdd, HSub, HAdd;

        VSub = 190;
        VAdd = 255;
        SSub = 190;
        SAdd = 255;
        HSub = 140;
        HAdd = 255;

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

//                takeScreenShot("jamhacks");
                click(340,80, "jamhacks");
                Thread.sleep(3000);


            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            Imgcodecs.imwrite("test.jpg", src);


            System.out.println(src.cols());

            src = Imgcodecs.imread("C:\\Users\\Brian-Laptop\\Downloads\\jamhacks.jpg");


            Core.flip(src, src, -1);

            //Mats needed for image processing
            Mat dst = new Mat();
            Mat out = new Mat();

            //Image filter values

            //resize the image to decrease the strain on the CPU
//            Imgproc.resize(src, src, new Size(src.cols() /4 , src.rows() / 4));

            Mat lol = src.colRange(0, src.cols() - (src.cols()/3));

            //make the high and low filter values
            Scalar hsvLow = new Scalar(HSub, SSub, VSub);
            Scalar hsvHigh = new Scalar(HAdd, SAdd, VAdd);

            //filter the image by Hue Saturation and Value then save the mask into dst
            Core.inRange(lol, hsvLow, hsvHigh, dst);

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
            lol.copyTo(out);

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

                if(Imgproc.minAreaRect(dank).size.area() > minSize && Imgproc.minAreaRect(dank).size.area() < maxSize) {
                    Point[] vertices = new Point[4];
                    Imgproc.minAreaRect(dank).points(vertices);
                    //System.out.println(determineBolt(calcSize(Imgproc.minAreaRect(dank).size.width, src.cols())) + " y = " + Double.toString(Imgproc.minAreaRect(dank).center.y));

//                    System.out.println("Size = " + Double.toString(calcSize(Imgproc.minAreaRect(dank).size.height, src.cols())) +
//                            " AR = " + determineAR(Imgproc.minAreaRect(dank).size.height, Imgproc.minAreaRect(dank).size.height) +
//                           " y = " + Double.toString(Imgproc.minAreaRect(dank).center.y));

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

            System.out.println("xPosFarRight = " + Double.toString(furthestRight));
            System.out.println("yPosFarRight = " + Double.toString(furthestRightY));
            System.out.println("furthestRightBoltSize = " + Double.toString(furthestRightBolt) + (Double.toString(furthestRight/3)));

            sendMessage(Integer.toString((int)(furthestRightBolt)));
            System.out.println(Integer.toString((int)(furthestRightBolt + (furthestRight / 100.0))));


            display(out);


//            wait 100ms to make the video viewable instead of quickly blinking past
            try {
                Thread.sleep(500);
                File file = new File("C:\\Users\\Brian-Laptop\\Downloads\\jamhacks.jpg");
                file.delete();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
