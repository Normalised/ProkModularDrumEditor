package com.korisnamedia.thonk;

import com.prokmodular.model.Preset;
import com.prokmodular.model.PresetManager;
import com.prokmodular.model.ProkModel;
import controlP5.ControlP5;
import controlP5.ScrollableList;
import controlP5.Textfield;
import processing.core.PGraphics;

import java.io.File;
import java.util.List;

import static processing.core.PApplet.println;

public class PresetManagerView {
    private final File prokDir;
    private final ControlP5 cp5;
    private final PGraphics graphics;
    private final ThonkModularApp app;

    private ScrollableList presetList;

    private Textfield presetNameInput;

    private PresetManager presetManager;

    public PresetManagerView(PGraphics graphics, ControlP5 cp5, ThonkModularApp thonkModularApp) {
        this.cp5 = cp5;
        this.graphics = graphics;
        app = thonkModularApp;

        prokDir = app.getDataDirectory();
        presetManager = new PresetManager();
    }


    public void setCurrentModel(ProkModel currentModel, File modelDir) {

        presetManager.setCurrentModel(currentModel, modelDir);

        println("Set Current Model " + currentModel.getConfig().getName());

        if(presetList != null) {
            listFiles();
        }
    }

    private void listFiles() {
        List<File> files = presetManager.listFiles();
        presetList.clear();

        for(File file : files) {
            presetList.addItem(file.getName(), file);
        }
    }

    public void createUI() {
        presetList = cp5.addScrollableList("Preset List", app.getWidth() - 200, 10, 180, app.getHeight() - 90);
        presetList.setType(ScrollableList.LIST);
        presetList.addListener(theEvent -> {
            File f = presetManager.getFileAtIndex((int) theEvent.getValue());

            if(cp5.isControlDown()) {
                savePreset(f);
            } else {
                println("List event " + theEvent.getName() + " : " + theEvent.getValue());
                loadPreset(f);
            }
        });
        presetList.setItemHeight(20);

        cp5.addButton("Save Preset")
                .setPosition(app.getWidth() - 160, app.getHeight() - 50)
                .setSize(100, 16)
                .onRelease(theEvent -> savePreset());

        presetNameInput = cp5.addTextfield("Preset Name", app.getWidth() - 200, app.getHeight() - 75, 180,20)
                .setCaptionLabel("");
        listFiles();
    }

    private void savePreset(File f) {
        println("Replacing existing preset " + f.getAbsolutePath());
        presetManager.savePreset(app.getPreset(), f);
    }

    private void savePreset() {
        println("Save Preset : " + presetNameInput.getText());
        if(presetNameInput.getText().length() > 0) {
            presetManager.savePreset(app.getPreset(), new File(app.getModelDirectory(), presetNameInput.getText() + ".prk"));
        }
    }

    private void loadPreset(File presetFile) {

        try {
            println("Loading preset file " + presetFile.getName());
            Preset p = presetManager.readPreset(presetFile);
            if(p != null) {
                app.applyPreset(p);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
