package com.korisnamedia.thonk.ui;

import com.korisnamedia.thonk.ThonkModularApp;
import com.prokmodular.model.Preset;
import com.prokmodular.model.PresetManager;
import com.prokmodular.model.ProkModel;
import controlP5.ControlP5;
import controlP5.ScrollableList;
import controlP5.Textfield;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import processing.core.PGraphics;

import java.awt.*;
import java.io.File;
import java.util.List;

import static processing.core.PApplet.selectFolder;

public class PresetManagerView {

    final Logger logger = LoggerFactory.getLogger(PresetManagerView.class);
    private final File prokDir;
    private final ControlP5 cp5;
    private final PGraphics graphics;
    private final ModuleEditorView app;

    private ScrollableList presetList;

    private Textfield presetNameInput;

    private PresetManager presetManager;
    private File patchFolder;

    public PresetManagerView(PGraphics graphics, ControlP5 cp5, ModuleEditorView view) {
        this.cp5 = cp5;
        this.graphics = graphics;
        app = view;

        prokDir = app.getDataDirectory();
        presetManager = new PresetManager();
        patchFolder = new File(prokDir, "patches");
    }


    public void setCurrentModel(ProkModel currentModel, File modelDir) {

        patchFolder = modelDir;
        presetManager.setCurrentModel(currentModel);

        logger.debug("Set Current Model " + currentModel.getConfig().getName());

        if(presetList != null) {
            listFiles();
        }
    }

    private void listFiles() {
        List<File> files = presetManager.listFilesFrom(patchFolder);
        presetList.clear();

        for(File file : files) {
            presetList.addItem(file.getName(), file);
        }
    }

    public void createUI() {
        cp5.addButton("Select Folder")
                .setPosition(app.getWidth() - 160, 20)
                .setSize(100, 16)
                .onRelease(theEvent -> selectPatchFolder());

        presetList = cp5.addScrollableList("Preset List", app.getWidth() - 200, 50, 180, app.getHeight() - 170);
        presetList.setType(ScrollableList.LIST);
        presetList.addListener(theEvent -> {
            File f = presetManager.getFileAtIndex((int) theEvent.getValue());

            if(cp5.isControlDown()) {
                savePreset(f);
            } else {
                logger.debug("List event " + theEvent.getName() + " : " + theEvent.getValue());
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

    private void selectPatchFolder() {
        selectFolder("Choose a folder", "folderChosen", patchFolder, this, (Frame) null);
        // String prompt, String callbackMethod, File defaultSelection, Object callbackObject, Frame parentFrame
    }

    public void folderChosen(File selectedFolder) {
        logger.debug("Folder Chosen " + selectedFolder.getAbsolutePath());
        patchFolder = selectedFolder;
        listFiles();
    }

    private void savePreset(File f) {
        logger.debug("Replacing existing preset " + f.getAbsolutePath());
        presetManager.savePreset(app.getPreset(), f);
        listFiles();
    }

    private void savePreset() {
        logger.debug("Save Preset : " + presetNameInput.getText());
        if(presetNameInput.getText().length() > 0) {
            presetManager.savePreset(app.getPreset(), new File(patchFolder, presetNameInput.getText() + ".prk"));
        }
        listFiles();
    }

    private void loadPreset(File presetFile) {

        try {
            logger.debug("Loading preset file " + presetFile.getName());
            Preset p = presetManager.readPreset(presetFile);
            if(p != null) {
                app.applyPreset(p);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
