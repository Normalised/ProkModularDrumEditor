package com.korisnamedia.thonk.ui;

import com.prokmodular.ProkModule;
import com.prokmodular.comms.CommandContents;
import com.prokmodular.comms.Messages;
import com.prokmodular.comms.ModuleCommandListener;
import controlP5.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import static com.korisnamedia.thonk.ui.ControlPanel.MORPH_ID;

public class MorphAndStorage implements ModuleCommandListener {

    final Logger logger = LoggerFactory.getLogger(MorphAndStorage.class);
    private final ControlP5 cp5;
    private final ModuleEditorView app;
    private ArrayList<QuadUI> quads;
    private Slider2D morphControl;
    private int blockHeight;
    private int blockWidth;
    private boolean saveMode = false;
    private Button saveButton;

    private ProkModule currentModule;
    private int x = 0;
    private int y = 40;
    private ArrayList<Button> clearButtons;

    public MorphAndStorage(ControlP5 cp5, ModuleEditorView view) {

        this.cp5 = cp5;
        this.app = view;
        blockHeight = 108;
        blockWidth = 108;

        logger.debug("Create MorphAndStorage");
        quads = new ArrayList<>();
        clearButtons = new ArrayList<>();

    }

    public void setModule(ProkModule module) {
        if(currentModule != null) {
            currentModule.removeCommandListener(this);
        }
        currentModule = module;
        currentModule.addCommandListener(this);
        setQuadState(currentModule.getProperty(Messages.QUAD_STATE));
        setSelectedQuad(currentModule.getProperty(Messages.QUAD_SELECT_INDEX));
    }

    public void createUI() {
        addMorph();
        addQuads();
    }

    private void addQuads() {
        // 4 x 4 squares

        int quadX = (int) (16 + x);
        int quadY = (int) (240 + y);

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
            Button clearButton = cp5.addButton("Clear" + i)
                    .setValue(0)
                    .setSize(40, 20)
                    .setLabel("Clear")
                    .setPosition(quadX + blockWidth + 4, quadY + (blockHeight - 42))
                    .setColorBackground(0xFFFF0000)
                    .onClick((CallbackEvent theEvent) -> {
                        clearQuad(index);
                    });

            clearButtons.add(clearButton);
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

        logger.debug("Quad Clicked " + row + ", " + col + ". Index " + index + ". Item Index " + itemIndex);

        if(saveMode) {
            app.saveModel((index * 4) + itemIndex);
            exitSaveMode();
        } else {
            app.selectModel((index * 4) + itemIndex);

            morphControl.setValue(col * 1024, (1 - row) * 1024);
            app.getCurrentParams();
            for (int i = 0; i < 4; i++) {
                quads.get(i).setSelected(i == index);
            }
        }

    }

    private void clearQuad(int index) {
//        logger.debug("Clear Quad " + index);
        app.clearQuad(index);
    }

    private void addMorph() {
        morphControl = cp5.addSlider2D("morph");

        morphControl.setId(MORPH_ID + 1)
                .setPosition(x , y + 20)
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

    @Override
    public void onCommand(CommandContents command) {
        if (command.is(Messages.QUAD_STATE)) {
            setQuadState(command.data);
        } else if (command.is(Messages.QUAD_SELECT_INDEX)) {
            setSelectedQuad(command.data);
        }
    }

    private void setSelectedQuad(String quadIndex) {
        if(quadIndex == null) return;
        if(quads.size() == 0) return;

        logger.debug("Set selected quad " + quadIndex);
        int bankIndex = Integer.parseInt(quadIndex);
        for (int i = 0; i < 4; i++) {
            quads.get(i).setSelected(i == bankIndex);
        }
    }

    private void setQuadState(String quadState) {
        if(quadState == null) return;

        if(quads.size() == 0) return;

        logger.debug("Set quad state " + quadState);
        int bankState = Integer.parseInt(quadState);
        for (int i = 0; i < 4; i++) {
            // Mask off bottom 4 bits
            quads.get(i).setState(bankState & 0xF);
            // Shift right 4
            bankState >>= 4;
        }
    }

    public void hide() {
        saveButton.hide();
        morphControl.hide();
        for (int i = 0; i < 4; i++) {
            quads.get(i).hide();
            clearButtons.get(i).hide();
        }
    }

    public void show() {
        saveButton.show();
        morphControl.show();
        for (int i = 0; i < 4; i++) {
            quads.get(i).show();
            clearButtons.get(i).show();
        }
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
