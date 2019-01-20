package com.korisnamedia.thonk.ui;

import com.korisnamedia.thonk.ConfigKeys;
import com.prokmodular.ProkModule;
import com.prokmodular.model.Preset;
import com.prokmodular.model.PresetManager;
import com.prokmodular.model.ProkModel;
import controlP5.*;
import controlP5.Button;
import org.kohsuke.randname.RandomNameGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.util.List;

import static processing.core.PApplet.selectFolder;

public class PresetManagerUI {

    final Logger logger = LoggerFactory.getLogger(PresetManagerUI.class);
    private final File prokDir;
    private final ControlP5 cp5;
    private final ModuleEditorView app;
    private File patchFolder;

    private ScrollableList presetList;
    private Textfield presetNameInput;

    private PresetManager presetManager;

    private ProkModel model;
    private Button savePresetButton;
    private Button selectFolderButton;
    private int x  = 0;
    private int y = 0;

    private RandomNameGenerator nameGenerator;

    public PresetManagerUI(ControlP5 cp5, ModuleEditorView view) {
        this.cp5 = cp5;
        app = view;

        nameGenerator = new RandomNameGenerator();

        prokDir = app.getDataDirectory();
        presetManager = new PresetManager();
        patchFolder = new File(app.getConfig().getString(ConfigKeys.PRESET_FOLDER));
        if(!patchFolder.exists()) {
            patchFolder.mkdirs();
        }

    }

    public void setModule(ProkModule module) {
        patchFolder = getModelDirectory(module);

        model = module.model;
        presetManager.setCurrentModel(model);

        logger.debug("Set Module " + model.getConfig().getName());

        if(presetList != null) {
            listFiles();
        }
    }

    public File getModelDirectory(ProkModule module) {
        File modelDir;
        if(app.getConfig().hasKey(module.getFilename() + ConfigKeys.PRESET_FOLDER)) {
            modelDir = new File(app.getConfig().getString(module.getFilename() + ConfigKeys.PRESET_FOLDER));
        } else {
            File patchesDir = new File(app.getConfig().getString(ConfigKeys.PRESET_FOLDER));
            modelDir = new File(patchesDir, module.getFilename());
            app.getConfig().setString(module.getFilename() + ConfigKeys.PRESET_FOLDER, modelDir.getAbsolutePath());
        }

        if(!modelDir.exists()) {
            boolean created = modelDir.mkdirs();
            if(!created) {
                logger.warn("Can't create presets folder in " + modelDir.getAbsolutePath());
                modelDir = app.getDataDirectory();
            }
        }


        return modelDir;
    }


    private void listFiles() {
        logger.debug("List Files " + patchFolder.getAbsolutePath());
        List<File> files = presetManager.listFilesFrom(patchFolder);
        presetList.clear();

        for(File file : files) {
            presetList.addItem(file.getName(), file);
        }
    }

    public void createUI() {
        selectFolderButton = cp5.addButton("Select Folder")
                .setPosition(x + 40, y)
                .setSize(100, 16)
                .onRelease(theEvent -> selectPatchFolder());

        presetList = cp5.addScrollableList("Preset List", x, y + 30, 180, app.getHeight() - 170);
        presetList.setType(ScrollableList.LIST);
        presetList.setBarVisible(false);
        presetList.onRelease(theEvent -> {
            File f = presetManager.getFileAtIndex((int) presetList.getValue());

            boolean controlDown = cp5.isControlDown() || cp5.isAltDown();
            logger.debug("Control down " + controlDown);
            if(controlDown) {
                replacePreset(f);
            } else {
                loadPreset(f);
            }
        });
        presetList.setItemHeight(20);

        savePresetButton = cp5.addButton("Save Preset")
                .setPosition(x + 40, app.getHeight() - 90)
                .setSize(100, 16)
                .onRelease(theEvent -> saveNewPreset());

        presetNameInput = cp5.addTextfield("Preset Name", x, presetList.getHeight() + 50, 180,20)
                .setCaptionLabel("");
//        listFiles();
    }

    private void selectPatchFolder() {
        selectFolder("Choose a folder", "folderChosen", patchFolder, this, (Frame) null);
        // String prompt, String callbackMethod, File defaultSelection, Object callbackObject, Frame parentFrame
    }

    public void folderChosen(File selectedFolder) {
        logger.debug("Folder Chosen " + selectedFolder.getAbsolutePath());
        patchFolder = selectedFolder;
        app.getConfig().setString(model.getConfig().filename + ConfigKeys.PRESET_FOLDER, patchFolder.getAbsolutePath());
        listFiles();
    }

    private void replacePreset(File f) {
        logger.debug("Replacing existing preset " + f.getAbsolutePath());
        presetManager.savePreset(app.getPreset(), f);
    }

    private void saveNewPreset() {
        logger.debug("Save Preset : " + presetNameInput.getText());
        String filename = presetNameInput.getText();
        if(filename.length() == 0) {
            filename = nameGenerator.next();
        }
        presetManager.savePreset(app.getPreset(), new File(patchFolder, filename + ".prk"));
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

    public void hide() {
        presetList.hide();
        presetNameInput.hide();
        selectFolderButton.hide();
        savePresetButton.hide();
    }

    public void show() {
        presetList.show();
        presetNameInput.show();
        selectFolderButton.show();
        savePresetButton.show();
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
