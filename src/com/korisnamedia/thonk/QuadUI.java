package com.korisnamedia.thonk;

import controlP5.ControlP5;
import controlP5.Controller;
import controlP5.ControllerView;
import processing.core.PGraphics;

import static processing.core.PApplet.dist;
import static processing.core.PApplet.println;

public class QuadUI extends Controller<QuadUI> {

    private int state = 0;

    private boolean selected = false;

    public QuadUI(ControlP5 cp5, String theName) {
        super(cp5, theName);
        setView(new BankUIView());
    }

    public void setState(int state) {
        this.state = state;
    }

    protected void onEnter() {
//        println("enter");
    }

    private class BankUIView implements ControllerView<QuadUI> {

        public void display( PGraphics graphics , QuadUI theController ) {

            int blockWidth = getWidth() - 8;
            int blockHeight = getHeight() - 8;

            if(selected) {
                graphics.fill(255,0,0);
                graphics.rect(- 2, - 2, blockWidth + 5, blockHeight + 5);
            }

            graphics.stroke(0);
            graphics.strokeWeight(1);
            graphics.fill(200,200,200);
            graphics.rect(0 , 0, blockWidth, blockHeight);

            int halfWidth = blockWidth / 2;
            int halfHeight = blockHeight / 2;

            boolean hasSaved = false;
            int[] shift = {0,1,3,2};

            for(int i=0;i<4;i++) {
                int col = i % 2;
                int row = i < 2 ? 1 : 0;
                if((state & (1 << shift[i])) > 0) {
                    graphics.fill(0,200,0);
                } else {
                    graphics.fill(200,200,200);
                }
                graphics.rect((col * halfWidth), (row * halfHeight), halfWidth, halfHeight);
            }
        }
    }

    public void setSelected(boolean s) {
        selected = s;
    }
}
