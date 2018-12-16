package com.korisnamedia.thonk;

import com.korisnamedia.thonk.ui.Layout;
import com.prokmodular.comms.SerialCommunicatorListener;
import controlP5.Button;
import controlP5.CallbackEvent;
import controlP5.ControlP5;
import controlP5.Slider2D;
import processing.core.PGraphics;

import java.util.ArrayList;

import static com.korisnamedia.thonk.ControlPanel.MORPH_ID;
import static processing.core.PApplet.println;

public class MorphAndStorage implements SerialCommunicatorListener {
    private final PGraphics graphics;
    private final ControlP5 cp5;
    private final ThonkModularApp app;
    private ArrayList<QuadUI> quads;
    private Slider2D morphControl;
    private int blockHeight;
    private int blockWidth;
    private boolean saveMode = false;
    private Button saveButton;
    private int x;
    private int y;

    private Layout layout;

    public MorphAndStorage(PGraphics graphics, ControlP5 cp5, ThonkModularApp app) {

        layout = new Layout();
        this.graphics = graphics;
        this.cp5 = cp5;
        this.app = app;
        blockHeight = 108;
        blockWidth = 108;

        println("Create MorphAndStorage");
        quads = new ArrayList<>();

        app.serialCommunicator.addSerialCommsListener(this);
    }

    public void create(int x, int y) {
        this.x = x;
        this.y = y;
        addMorph();
        addQuads();
    }

    private void addQuads() {
        // 4 x 4 squares

        int quadX = 16 + x;
        int quadY = 240 + y;

        saveButton = cp5.addButton("Save To");
        saveButton.setValue(0)
                .setSize(60, 20)
                .setLabel("Save To")
                .setPosition(quadX + 22, quadY)
                .onRelease(theEvent -> {
                    if(!saveMode) {
                        enterSaveMode();
                    } else {
                        exitSaveMode();
                    }
                });
        saveButton.setColorBackground(0xFFA0A0A0).setColorLabel(0xFFFFFFFF);

        quadY += 30;

        for (int i = 0; i < 4; i++) {
            int index = i;

            QuadUI quadUI = new QuadUI(cp5, "Quad" + i);
            quadUI.setSize(blockWidth, blockHeight)
                    .setPosition(quadX, quadY)
                    .onRelease((CallbackEvent theEvent) -> {
                        int mx = theEvent.getController().getPointer().x();
                        int my = theEvent.getController().getPointer().y();

                        quadClicked(index, mx, my);
                    });
            cp5.addButton("Clear" + i)
                    .setValue(0)
                    .setSize(40, 20)
                    .setLabel("Clear")
                    .setPosition(quadX + blockWidth + 4, quadY + (blockHeight - 42))
                    .setColorBackground(0xFFFF0000)
                    .onClick((CallbackEvent theEvent) -> {
                        clearQuad(index);
                    });

//            cp5.addButton("Save"+i)
//                    .setValue(0)
//                    .setSize(40, 20)
//                    .setLabel("Save")
//                    .setPosition(quadX + blockWidth + 4, quadY + 16)
//                    .setColorBackground(0xFF009900)
//                    .onClick((CallbackEvent theEvent) -> {
//                        saveQuad(index);
//                    });
            quadY += blockHeight + 4;
            quads.add(quadUI);
        }

    }

    private void saveQuad(int index) {

    }

    private void enterSaveMode() {
        saveMode = true;
        saveButton.setColorBackground(0xFF00FF00).setColorLabel(0);
    }

    private void exitSaveMode() {
        saveMode = false;
        saveButton.setColorBackground(0xFFA0A0A0).setColorLabel(0xFFFFFFFF);
    }

    private void quadClicked(int index, int mx, int my) {

        int row = my / (blockHeight / 2);
        int col = mx / (blockWidth / 2);
        int itemIndex = row == 0 ? (col == 0 ? 3 : 2) : (col == 0 ? 0 : 1);

        println("Quad Clicked " + row + ", " + col + ". Index " + index + ". Item Index " + itemIndex);

        if(saveMode) {
//            println("Saving current to " + index + " : " + itemIndex);
            app.saveModel((index * 4) + itemIndex);
            exitSaveMode();
        } else {
//            println("Selecting model " + index + " : " + itemIndex);
            app.selectModel((index * 4) + itemIndex);

            morphControl.setValue(col * 1024, (1 - row) * 1024);
            app.getCurrentParams();
            for (int i = 0; i < 4; i++) {
                quads.get(i).setSelected(i == index);
            }
        }

    }

    private void clearQuad(int index) {
//        println("Clear Quad " + index);
        app.clearQuad(index);
    }

    private void addMorph() {
        morphControl = cp5.addSlider2D("morph");

        morphControl.setId(MORPH_ID + 1)
                .setPosition(layout.topMargin + x , layout.topMargin + y)
                .setSize(200, 200)
                .setMinMax(0, 1024, 1024, 0)
                .setValue(0, 0)
                .onRelease((CallbackEvent theEvent) -> {
                    app.getCurrentParams();
                })
                .onReleaseOutside((CallbackEvent theEvent) -> {
                    app.getCurrentParams();
                })
                .onEndDrag((CallbackEvent theEvent) -> {
                    app.getCurrentParams();
                });

    }

    private void createQuickSaveButtons() {
    }

    @Override
    public void serialConnected(String type) {
        // dont care
    }

    @Override
    public void serialDisconnected() {

    }

    @Override
    public void onData(String propName, String propValue) {
        if (propName.equalsIgnoreCase("quadState")) {
            int bankState = Integer.parseInt(propValue);
            for (int i = 0; i < 4; i++) {
                // Mask off bottom 4 bits
                quads.get(i).setState(bankState & 0xF);
                // Shift right 4
                bankState >>= 4;
            }
        } else if (propName.equalsIgnoreCase("quad")) {
            int bankIndex = Integer.parseInt(propValue);
            for (int i = 0; i < 4; i++) {
                quads.get(i).setSelected(i == bankIndex);
            }
        }

//        else if(propName.equalsIgnoreCase("morph")) {
//            String[] parts = propValue.split(",");
//            if(parts.length == 2) {
//                float x = Float.parseFloat(parts[0]);
//                float y = Float.parseFloat(parts[1]);
//                morphControl.setBroadcast(false);
//                morphControl.setValue(x,y);
//                morphControl.setBroadcast(true);
//            }
//        }
    }
}
