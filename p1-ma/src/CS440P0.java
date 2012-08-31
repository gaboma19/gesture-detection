import java.awt.Image;
import java.awt.Color;

/***********
  * The VideoSink class represents the entry point for high level
  * analysis of videos. Images are fed to the CS585Hw1 class via 
  * the receiveFrame. The videoSink has the ability to display 
  * images with the ImageViewer class.
  * 
  * @author Sam Epstein
  **********/
public class CS440P0 {
    //The window to display images
    ImageViewer imageViewer;
    
    //Simple counter for video cutoff
    long counter;
    int numFrames = 20;
    int[] xCentDiff = new int[6];
    int[] yCentDiff = new int[6];
    int[] areasDiff = new int[numFrames];
    int frameCount=0;
    int whiteCountX=0;
    int whiteCountY=0;
    Color white=Color.white;
    Color red=Color.red;
    Color black=Color.black;
    
    String wave = "";
    String oldWave = "";
    int alt = 0;
    int switches = 0;
    
    double probFist = 0;
    double probWave = 0;
    double probDepth = 0;
    
    //The constructor initializes the window for display
    CS440P0()
    {
        imageViewer=new ImageViewer("Image Viewer");
        counter = 0;
    }
    
    /**
     * The central function of VideoSink and the place where students
     * can edit the code. receiveFrame function is given an image. The 
     * body of the code will perform high level manipulations of the 
     * image, then display the image in the imageViewer. The return values
     * indicates to the the video source whether or not to keep sending 
     * images.
     * 
     * @param frame The current frame of the video source/
     * @param firstFrame Whether or not the frame is the first frame of the video
     * @return true if the video source should continue, or false if the video source should stop.
     */
    public boolean receiveFrame(CS440Image frame) 
    {
        int width = frame.width();
        int height = frame.height();
        int xCounter=0;
        int yCounter=0;
        int xTotal=1;
        int yTotal=1;
        Color c;
        Color[][] image = new Color[width][height];
        
        //Searches through each pixel in frame and calls isSkinTone() function to determine the pixel's relevance
        for(int x=0; x<width; x++)
        {
            for(int y=0; y<height; y++)
            {     
                c=frame.get(x,y);
                if(isSkinTone(x, y, c))
                {
                    xCounter+=x;
                    yCounter+=y;
                    xTotal++;
                    yTotal++;
                    image[x][y] = c;
                    frame.set(x,y,c);
                }
                else
                {
                    image[x][y] = c;
                    frame.set(x,y,c);
                }
            }
        }
        
        //Determining coordinates of centroid of object
        int centX=((xCounter)/xTotal);
        int centY=((yCounter)/yTotal);
        
        //Determining height of object
        for(int y=0; y<height; y++)
        {         
            c=image[centX][y];
            if(isSkinTone(centX, y, c))
            {
                whiteCountX++;  
            }
        }
        
        //Determining width of object
        for(int x=0; x<width; x++) 
        {
            c=image[x][centY];
            if(isSkinTone(x, centY, c))
            {
                whiteCountY++;
            }
        }
        
        int bb1x = centX-(whiteCountY/2);
        int bb1y = centY-(whiteCountX/2);
        int bb4x = centX+whiteCountY/2;
        int bb4y = centY+whiteCountX/2;
        int l = bb4x - bb1x;
        int h = bb4y - bb1y;
        int area = l*h;      
        areasDiff[frameCount%numFrames] = area;

        //Function for Bounding Box
        for(int x1=bb1x; x1<bb4x; x1++)
        {
            if(x1<width && x1 >= 0) 
            {
                for(int y1=bb1y; y1<bb4y; y1++)
                {
                    if (y1 < height && y1 >= 0)
                    {
                        if (image[x1][y1]!=null) 
                        {
                            Color curr = image[x1][y1];
                            int newR = 255-curr.getRed();
                            int newG = 255-curr.getGreen();
                            int newB = 255-curr.getBlue();
                            Color negative = new Color(newR, newG, newB);
                            frame.set(x1, y1, negative);
                        }
                    }
                }
            }
        }
        
        detectDepth(centX, centY);
        detectFist(h, l);
        detectMovements(centX, centY);
        
        
        System.out.println("Wave " + probWave + "Fist " + probFist + "Depth " + probDepth);
        
        probWave = 0;
        probFist = 0;
        probDepth = 0;
        
        boolean shouldStop = displayImage(frame); 
        return shouldStop;
    }
    
    /**
     * This function determines whether the color of a pixel matches a skin tone.
     * The input parameters include the original color and the coordinates of the pixel.
     */
    public boolean isSkinTone(int x, int y, Color c) 
    {
        int r=c.getRed();
        int b=c.getBlue();
        int g=c.getGreen();
        int maxv = Math.max(r, (Math.max(b,g)));
        int minv = Math.min(r, (Math.min(b,g)));
        return (r>95)&&(g>40)&&(b>20) && (maxv-minv>15) && Math.abs(r-g)>15 && r>g && r>b;
    }
    
    /**
     * This function is used to detect movements between frames of an image.
     * The two parameters, centX and centY are the coordinates of the centroid of an object in the frame.
     */
    public void detectMovements(int centX, int centY) 
    {
        int pixelChange = 50;
        for(int i=0;i<=5;i++)
        {
            if(xCentDiff[i]==0)
            { 
                xCentDiff[i]=centX;
                yCentDiff[i]=centY;
                
                if(i==5) 
                {
                    if((xCentDiff[5]-xCentDiff[0])<-pixelChange)
                    {
                        System.out.println("LEFT");
                        if (alt%2== 0) { oldWave = "left"; }
                        else { wave = "left"; }
                        
                        if (!oldWave.equals(wave)) {
                            if (switches == 2) {
                                probWave = 1;
                                switches = 0;
                            } else {
                                switches++;
                            }
                        }
                    } 
                    else if(xCentDiff[5]-xCentDiff[0]>pixelChange)
                    {
                        System.out.println("RIGHT");
                        if (alt%2==0) { oldWave = "right"; }
                        else { wave = "right"; }
                        
                        if (!oldWave.equals(wave)) {
                            if (switches == 2) {
                                probWave = 1;
                                switches = 0;
                                
                            } else {
                                switches++;
                            }
                        }
                    }
                    xCentDiff=new int[6];
                    yCentDiff=new int[6];       
                }
                break;
            }
            alt++;
        }
        frameCount++;
        whiteCountX=0;
        whiteCountY=0;
        
    }
    public void detectDepth(int centX, int centY) 
    {
        int pixelChange = 10000;
        
        if (frameCount > numFrames) {
            int currArea = areasDiff[frameCount%numFrames];
            int oldArea = areasDiff[(frameCount-1)%numFrames];
            if(Math.abs(currArea-oldArea)>pixelChange)
            {
                probDepth = .9;
            } 
            else if (Math.abs(currArea - oldArea) > 5000) {
            	probDepth = .5;
            }
            else if (Math.abs(currArea - oldArea) > 1000) {
            	probDepth = .2;
            }
            else {
            	probDepth = 0;
            }
        }
        if (frameCount%numFrames==0)
        {
        	areasDiff=new int[numFrames];    
        }
    }

    public void detectFist(int h, int l) 
    {
        if (h > 1.4 * l) {
            probFist = .1;
    	} else if (l > 2 * h) {
            probFist = .7;
        }  
    }
    
    /**
     * This function displays the passed image in a frame.
     * @param image The image to be displayed
     */
    public boolean displayImage(CS440Image image)
    {
        // Window is closed.
        if (!imageViewer.isShowing())
        {
            return false;
        }
        
        if(imageViewer == null) 
        {
            // System.out.println("now we return false");
            return false;   
        }
        
        imageViewer.showImage(image);
        return true;
    }
    
    /***
      * Closes the window
      */
    public void close()
    {
        if(imageViewer!=null)
        {
            this.imageViewer.dispose();
            imageViewer=null;
        }
    }
    
}
