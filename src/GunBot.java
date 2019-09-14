import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;

public class GunBot {

	/*
	private static long t;
	private static long lastT = System.currentTimeMillis();
	private static long nFrames;
	private static long avgFrameT;
	*/

    //robot vars
    private static Robot robot;
    private static boolean running = false;

    //common key codes
    private static final int W = KeyEvent.VK_W;
    private static final int A = KeyEvent.VK_A;
    private static final int S = KeyEvent.VK_S;
    private static final int D = KeyEvent.VK_D;

    //grid vars
    private static final int X_OFFSET = 577;
    //windowed
    private static final int Y_OFFSET = 368;
    //fullscreen
    //private static final int Y_OFFSET = 347;

    private static final int CELL_WIDTH = 32;

    //screen pixel vars
    private static Rectangle screenRect;
    private static BufferedImage grid;
    private static int tst[];
    private static Raster k;

    //game vars
    private static byte[] gunStatus = new byte[4];
    private static Vector3Int[] arrows = new Vector3Int[3];

    //image values
    //blue background color(44,95,122) @ 500,500
    //cannon
    //clean-empty (255,255,255)
    //firing (128,128,128)
    //dirty-empty (0,0,0)
    //powder (255,0,0)
    //wad (0,255,0)
    //shot (255,0,255)
    //wash-bucket (0,0,255)

    //set clipboard contents
    private static void copy(String text) {
        StringSelection stringSelection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, stringSelection);
    }

    //paste clipboard contents to current selection
    private static void paste() {
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        robot.keyRelease(KeyEvent.VK_V);
    }

    //press a key down and lift it up
    private static void press(int keyEventCode) {
        robot.keyPress(keyEventCode);
        robot.keyRelease(keyEventCode);
    }

    //convert grid position to mouse coords
    private static int gridToMouseX(double gridX) {
        return (int) (X_OFFSET + (gridX*CELL_WIDTH) + (CELL_WIDTH/2));
    }
    private static int gridToMouseY(double gridY) {
        return (int) (Y_OFFSET + (gridY*CELL_WIDTH) + (CELL_WIDTH/2));
    }

    //click the designated location on the screen
    private static void click(int x, int y) {
        robot.mouseMove(x, y);
        //don't click until mouse is at postion
        while(MouseInfo.getPointerInfo().getLocation().x != x){
            continue;
        }
        while(MouseInfo.getPointerInfo().getLocation().y != y){
            continue;
        }
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    //move the mouse back to the center of the screen to minimize travel time
    private static void recenterMouse(){
        robot.mouseMove(gridToMouseX(6), gridToMouseY(6));
    }

    //create an arrow of the specified direction at the specified location
    private static void createArrow(int gridX, int gridY, int key){

        press(key);
        robot.waitForIdle();
        robot.delay(6);
        click(gridToMouseX(gridX),gridToMouseY(gridY));
        robot.waitForIdle();
        robot.delay(6);

    }

    //check if arrow currently exists
    private static boolean arrowExists(int gridX, int gridY, int key) {
        switch(key){
            case W:
                return(isColorAt(gridX-.45,gridY-.45,255,255,0)||
                   isColorAt(gridX-.45,gridY-.45,202,215,31)||
                   isColorAt(gridX-.45,gridY-.45,150,175,61));
            case A:
                return(isColorAt(gridX-.45,gridY-.45,191,191,0)||
                        isColorAt(gridX-.45,gridY-.45,154,167,31)||
                        isColorAt(gridX-.45,gridY-.45,118,143,61));
            case S:
                return(isColorAt(gridX-.45,gridY-.45,128,128,0)||
                        isColorAt(gridX-.45,gridY-.45,107,120,31)||
                        isColorAt(gridX-.45,gridY-.45,86,11,61));
            case D:
                return(isColorAt(gridX-.45,gridY-.45,64,64,0)||
                        isColorAt(gridX-.45,gridY-.45,59,72,31)||
                        isColorAt(gridX-.45,gridY-.45,54,79,61));
            default:
                return false;
        }
    }

    //scan cells for specific item
    private static boolean isColorAt(double gridX, double gridY, int r, int g, int b) {

        //check center-most pixel of cell

        tst = k.getPixel((int)(gridX*CELL_WIDTH)+CELL_WIDTH/2,(int)(gridY*CELL_WIDTH)+CELL_WIDTH/2,tst);
        if(tst[0]!=r || tst[1]!=g || tst[2]!=b) {
            return false;
        }else{
            return true;
        }

/*
        //check the center four pixels of the cell
        for(int x=gridX*CELL_WIDTH+CELL_WIDTH/2-1;x<gridX*CELL_WIDTH+CELL_WIDTH/2+1;x++) {
            for(int y=gridY*CELL_WIDTH+CELL_WIDTH/2-1;y<gridY*CELL_WIDTH+CELL_WIDTH/2+1;y++) {
                tst = k.getPixel(x,y,tst); //x&y are now the pixel coords relative to the cell
                //System.out.println("B:"+tst[1]);
                if(tst[0]!=r || tst[1]!=g || tst[2]!=b) {
                    return false;
                }
            }
        }
        return true;
*/
    }

    //checks that the bot should be running currently
    //looks for the barrel and the background
    private static boolean checkGameActive() {
        //check that barrel is visible (game not paused)
        if(isColorAt(6,6,0,0,0)) {
        }else {
            return false;
        }

        //check that background is visible (not just looking at some white/gray/black on computer as cannon)
        if(!robot.getPixelColor(500, 500).equals(new Color(44,95,122))) {
            return false;
        }

        return true;
    }

    //get entity at specified grid location
        //0->shot
        //1->water
        //4->paper
        //5->powder
        //3->not relevant
    private static byte getEntityAt(double gridX, double gridY){
        if(isColorAt(gridX,gridY,255,0,0)){
            return 5;
        }else if(isColorAt(gridX,gridY,0,255,0)){
            return 4;
        }else if(isColorAt(gridX,gridY,255,0,255)){
            return 0;
        }else if(isColorAt(gridX,gridY,0,0,255)){
            return 1;
        }else{
            return 3;
        }
    }

    //get gun status
        //0->needs to be filled (or needs cannon)
        //1->needs to be cleaned
        //2->is on standby
        //3->wtf? (error)
        //4->needs paper
        //5->needs powder
    private static byte getGunStatus(int gridX, int gridY) {

        if(isColorAt(gridX,gridY,255,255,255)) {
            //needs to be filled
            return 0;
        }else if(isColorAt(gridX,gridY,0,0,0) || isColorAt(gridX,gridY,255,0,0) || isColorAt(gridX,gridY,0,255,0)) {
            //needs to be cleaned
            return 1;
        }else if(isColorAt(gridX,gridY,128,128,128)) {
            //is on standby
            return 2;
        }else {
            //wtf? (error)
            return 3;
        }
    }
    private static void specifyGunStatus() {

        //designate what each gun needs (if front is empty)

        //gun0
        if(gunStatus[0]==0) {
            //what is in the middle of the cannon?
                //is it empty?
            if(isColorAt(2,3,255,255,255)){
                //is there powder in the back?
                if(isColorAt(3,3,255,0,0)){
                    //then needs paper
                    gunStatus[0]=4;
                //if the back has something other than powder, needs to be washed
                }else if(!isColorAt(3,3,255,255,255)){
                    gunStatus[0]=1;
                //otherwise the back needs powder
                }else{
                    gunStatus[0]=5;
                }
            //if the middle has something other than powder, needs to be washed
            }else if(!isColorAt(2,3,0,255,0)){
                gunStatus[0]=1;
            }
            //otherwsie the front needs a shot
            else{
                gunStatus[0]=0;
            }
        }

        //gun1
        if(gunStatus[1]==0) {
            //what is in the middle of the cannon?
            //is it empty?
            if(isColorAt(10,3,255,255,255)){
                //is there powder in the back?
                if(isColorAt(9,3,255,0,0)){
                    //then needs paper
                    gunStatus[1]=4;
                    //if the back has something other than powder, needs to be washed
                }else if(!isColorAt(9,3,255,255,255)){
                    gunStatus[1]=1;
                    //otherwise the back needs powder
                }else{
                    gunStatus[1]=5;
                }
                //if the middle has something other than powder, needs to be washed
            }else if(!isColorAt(10,3,0,255,0)){
                gunStatus[1]=1;
            }
            //otherwsie the front needs a shot
            else{
                gunStatus[1]=0;
            }
        }

        //gun2
        if(gunStatus[2]==0) {
            //what is in the middle of the cannon?
            //is it empty?
            if(isColorAt(2,9,255,255,255)){
                //is there powder in the back?
                if(isColorAt(3,9,255,0,0)){
                    //then needs paper
                    gunStatus[2]=4;
                    //if the back has something other than powder, needs to be washed
                }else if(!isColorAt(3,9,255,255,255)){
                    gunStatus[2]=1;
                    //otherwise the back needs powder
                }else{
                    gunStatus[2]=5;
                }
                //if the middle has something other than powder, needs to be washed
            }else if(!isColorAt(2,9,0,255,0)){
                gunStatus[2]=1;
            }
            //otherwsie the front needs a shot
            else{
                gunStatus[2]=0;
            }
        }

        //gun3
        if(gunStatus[3]==0) {
            //what is in the middle of the cannon?
            //is it empty?
            if(isColorAt(10,9,255,255,255)){
                //is there powder in the back?
                if(isColorAt(9,9,255,0,0)){
                    //then needs paper
                    gunStatus[3]=4;
                    //if the back has something other than powder, needs to be washed
                }else if(!isColorAt(9,9,255,255,255)){
                    gunStatus[3]=1;
                    //otherwise the back needs powder
                }else{
                    gunStatus[3]=5;
                }
            //if the middle has something other than powder, needs to be washed
            }else if(!isColorAt(10,9,0,255,0)){
                gunStatus[3]=1;
            }
            //otherwsie the front needs a shot
            else{
                gunStatus[3]=0;
            }
        }

        /*
		for(int i=3;i<4;i++) {
			switch(gunStatus[i]) {
				case 0:
					System.out.println("Gun "+i+" needs shot.");
					break;
				case 1:
					System.out.println("Gun "+i+" needs water.");
					break;
				case 2:
					System.out.println("Gun "+i+" is on standby.");
					break;
				case 3:
					System.out.println("Gun "+i+" is fucked.");
					break;
				case 4:
					System.out.println("Gun "+i+" needs wad.");
					break;
				case 5:
					System.out.println("Gun "+i+" needs powder.");
					break;
			}
		}
        */
    }

    //main method
    public static void main(String[] args) throws IOException, AWTException {

        //init robot stuff
        robot = new Robot();

        //init screen capture stuff
        screenRect = new Rectangle(X_OFFSET, Y_OFFSET, CELL_WIDTH*13, CELL_WIDTH*13);
        grid = robot.createScreenCapture(screenRect);
        tst = null;
        k = grid.getData();

        System.out.println("Bot started.");
        running = true;

        //run robot indefinitely
        while(true) {

            ///*
            //System.out.print("X: "+(MouseInfo.getPointerInfo().getLocation().x));//-X_OFFSET));
            //System.out.println(", Y: "+(MouseInfo.getPointerInfo().getLocation().y-Y_OFFSET));
            //*/
			///*
			//t = System.currentTimeMillis();
			//avgFrameT = ((t-lastT)+(avgFrameT)*nFrames) / (nFrames+1);
			//nFrames++;
			//lastT = t;
			//System.out.println(avgFrameT);
			//*/

            //re-capture screen pixels
            grid = robot.createScreenCapture(screenRect);
            k = grid.getData();
            tst = k.getPixel(48,110,tst);

            //check that puzzle is active (look for cannon && blue background)
            if(!checkGameActive()) {
                if(running){
                    arrows[0]=null;
                    arrows[1]=null;
                    arrows[2]=null;
                    running = false;
                    System.out.println("Stopped...");
                }
                robot.delay(10);
                continue;
            }
            if(!running){
                recenterMouse();
                running = true;
                System.out.println("Resumed...");
            }

            //refresh TL [0,5 (D)] and BR [12,7 (A)] loop spots if needed
            if(!arrowExists(0,5,D)){
                createArrow(0,5,D);
                recenterMouse();
            }
            if(!arrowExists(12,7,A)){
                createArrow(12,7,A);
                recenterMouse();
            }

            //check if position two above or two below barrel is (about to be) occupied with a piece
            if(isColorAt(6,4.55,255,0,0) || isColorAt(6,4.55,0,255,0) ||
                    isColorAt(6,4.55,0,0,255) ||isColorAt(6,4.55,255,0,255)) {
                createArrow(6,4,S);
                createArrow(6,5,D);
                recenterMouse();
            }
            if(isColorAt(6,7.45,255,0,0) || isColorAt(6,7.45,0,255,0) ||
                    isColorAt(6,7.45,0,0,255) ||isColorAt(6,7.45,255,0,255)) {
                createArrow(6,8,W);
                createArrow(6,7,A);
                recenterMouse();
            }

            //check gun status
            //top left
            gunStatus[0] = getGunStatus(1,3);
            //top right
            gunStatus[1] = getGunStatus(11,3);
            //bot left
            gunStatus[2] = getGunStatus(1,9);
            //bot right
            gunStatus[3] = getGunStatus(11,9);

            //specify gun status for guns needing to be filled
            specifyGunStatus();

            //get each gun what it needs
            byte loop, entryL, entryU;

            //gun 0
            if(gunStatus[0]!=2) {
                loop = getEntityAt(0, 5.45);
                entryU = getEntityAt(0, 2.65);
                entryL = getEntityAt(0, 3.45);
                if (loop == gunStatus[0]) {
                    createArrow(0, 5, W);
                    recenterMouse();
                }
                if (entryL == gunStatus[0] || entryU == gunStatus[0]) {
                    createArrow(0, 3, D);
                    recenterMouse();
                } else if ((entryL != 3 || entryU != 3) && arrowExists(0,3,D)) {
                    createArrow(0, 3, S);
                    recenterMouse();
                }
            }
            //gun1
            else if(gunStatus[1]!=2){
                loop = getEntityAt(11.55, 5);
                entryU = getEntityAt(12, 2.65);
                entryL = getEntityAt(12, 3.45);
                if (loop == gunStatus[1]) {
                    createArrow(12, 5, W);
                    recenterMouse();
                }else if(loop != 3 && arrowExists(12,5,W)){
                    createArrow(12, 5, S);
                    recenterMouse();
                }

                if (entryL == gunStatus[1] || entryU == gunStatus[1]) {
                    createArrow(12, 3, A);
                    recenterMouse();
                } else if ((entryL != 3 || entryU != 3) && arrowExists(12,3,A)) {
                    createArrow(12, 3, S);
                    recenterMouse();
                }
            }

            //gun 3
            if(gunStatus[3]!=2) {
                loop = getEntityAt(12, 6.55);
                entryU = getEntityAt(12, 8.55);
                entryL = getEntityAt(12, 8.55);
                if (loop == gunStatus[3]) {
                    createArrow(12, 7, S);
                    recenterMouse();
                }
                if (entryL == gunStatus[3] || entryU == gunStatus[3]) {
                    createArrow(12, 9, A);
                    recenterMouse();
                } else if ((entryL != 3 || entryU != 3) && arrowExists(0,3,A)) {
                    createArrow(0, 3, W);
                    recenterMouse();
                }
            }
            //gun2
            else if(gunStatus[2]!=2){
                loop = getEntityAt(0.45, 7);
                entryU = getEntityAt(0, 8.65);
                entryL = getEntityAt(0, 9.45);
                if (loop == gunStatus[2]) {
                    createArrow(0, 7, S);
                    recenterMouse();
                }else if (loop != 3 && arrowExists(0,7,S)) {
                    createArrow(0, 7, W);
                    recenterMouse();
                }
                if (entryL == gunStatus[2] || entryU == gunStatus[2]) {
                    createArrow(0, 9, D);
                    recenterMouse();
                } else if ((entryL != 3 || entryU != 3) && arrowExists(0,9,D)) {
                    createArrow(0, 9, W);
                    recenterMouse();
                }
            }

        }
    }

}