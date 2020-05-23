package com.korisnamedia.thonk.ui;

import com.korisnamedia.thonk.ConfigKeys;
import com.prokmodular.ProkModule;
import com.prokmodular.model.Preset;
import com.prokmodular.model.PresetFile;
import com.prokmodular.model.PresetManager;
import com.prokmodular.model.ProkModel;
import controlP5.*;
import controlP5.Button;
import controlP5.Label;
import org.kohsuke.randname.RandomNameGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import processing.core.PGraphics;

import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.Map;

import static controlP5.ControlP5.b;
import static processing.core.PApplet.selectFolder;

public class PresetManagerUI implements ScrollableList.ListItemRenderer {

    final Logger logger = LoggerFactory.getLogger(PresetManagerUI.class);
    private final File prokDir;
    private final ControlP5 cp5;
    private final ModuleEditorView app;
    private File patchFolder;
    private File scratchFolder;

    private ScrollableList presetList;
    private Textfield presetNameInput;

    private PresetManager presetManager;

    private ProkModel model;
    private Button savePresetButton;
    private Button selectFolderButton;
    //private Button scratchFolderButton;

    private int x  = 0;
    private int y = 0;

    private RandomNameGenerator nameGenerator;

    public PresetManagerUI(ControlP5 cp5, ModuleEditorView view) {
        this.cp5 = cp5;
        //cp5.disableShortcuts();
        app = view;

        nameGenerator = new RandomNameGenerator();

        prokDir = app.getDataDirectory();
        presetManager = new PresetManager();
        patchFolder = new File(app.getConfig().getString(ConfigKeys.PRESET_FOLDER));
        if(!patchFolder.exists()) {
            patchFolder.mkdirs();
        }

        File patchParent = new File(app.getConfig().getString(ConfigKeys.PRESET_FOLDER)).getParentFile();
        scratchFolder = new File(patchParent, "scratch");
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

        long start = System.currentTimeMillis();
        List<PresetFile> files = presetManager.listFilesFrom(patchFolder);
        presetList.clear();

        for(PresetFile presetFile : files) {
            presetList.addItem(presetFile.getName(), presetFile);
        }

        long end = System.currentTimeMillis();
        logger.debug("List files took " + (end - start));
    }

    public void createUI() {
        selectFolderButton = cp5.addButton("Choose Patch Folder")
                .setPosition(x + 40, y)
                .setSize(100, 16)
                .onRelease(theEvent -> selectPatchFolder());

//        scratchFolderButton = cp5.addButton("Select Scratch")
//                .setPosition(x + 40, app.getHeight() - 60)
//                .setSize(100, 16)
//                .onRelease(theEvent -> selectScratchFolder());

        presetList = cp5.addScrollableList("Preset List", x, y + 30, 180, app.getHeight() - 170);
        ScrollableList.ScrollableListView view = (ScrollableList.ScrollableListView) presetList.getView();
        view.itemRenderer = this;
        presetList.setType(ScrollableList.LIST);
        presetList.setBarVisible(false);
        presetList.onRelease(theEvent -> {
            File f = presetManager.getFileAtIndex((int) presetList.getValue());

            boolean controlDown = cp5.isControlDown();
            boolean altDown = cp5.isAltDown();
            logger.debug("Control down " + controlDown + ". Alt down " + cp5.isAltDown());
            if(controlDown) {
                replacePreset(f);
            }
            else if(altDown) {
                savePresetToScratch(f);
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

    private void savePresetToScratch(File f) {
        File scratchFile = new File(scratchFolder, f.getName());
        logger.debug("Saving to scratch file " + scratchFile.getAbsolutePath());
        presetManager.savePreset(app.getPreset(), scratchFile);
    }

    private void selectScratchFolder() {
        selectFolder("Choose a scratch folder", "scratchFolderChosen", scratchFolder, this, (Frame) null);
    }

    private void selectPatchFolder() {
        selectFolder("Choose a folder", "folderChosen", patchFolder, this, (Frame) null);
    }

    public void folderChosen(File selectedFolder) {
        if(selectedFolder == null) return;
        logger.debug("Folder Chosen " + selectedFolder.getAbsolutePath());
        patchFolder = selectedFolder;
        app.getConfig().setString(model.getConfig().filename + ConfigKeys.PRESET_FOLDER, patchFolder.getAbsolutePath());
        listFiles();
    }

    public void scratchFolderChosen(File selectedFolder) {
        if(selectedFolder == null) return;
        logger.debug("Scratch Folder Chosen " + selectedFolder.getAbsolutePath());
        scratchFolder = selectedFolder;
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

    public void loadPreset(File presetFile) {

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
//        scratchFolderButton.hide();
    }

    public void show() {
        presetList.show();
        presetNameInput.show();
        selectFolderButton.show();
        savePresetButton.show();
//        scratchFolderButton.show();
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void drawItem(PGraphics g, Map<String, Object> item, boolean isMouseOver, boolean isMousePressed, int itemWidth, int itemHeight, Label label) {
        CColor color = (CColor) item.get("color");

        PresetFile pf = (PresetFile) item.get("value");
        int presetVersion = pf.preset.config.getVersion();
        int modelVersion = model.getConfig().getVersion();

        g.fill((b(item.get("state"))) ? color.getActive() : isMouseOver ? (isMousePressed ? color.getActive() : color.getForeground()) : color.getBackground());

        g.rect(0, 0, itemWidth - 20, itemHeight - 1);


        // Colours are ARGB
        int sameGreen = 0xFF1EBB20;

        if(presetVersion == modelVersion) {

            g.fill(sameGreen);
        } else if(presetVersion < modelVersion) {
            g.fill(ControlP5.PURPLE);
        } else {
            g.fill(ControlP5.RED);
        }


        g.rect(itemWidth - 18, 0, 18, itemHeight - 1);

        label.set(item.get("text").toString()).draw(g, 4, itemHeight / 2);

        String version = String.valueOf(presetVersion);
        label.set(version).draw(g, itemWidth - 12, itemHeight / 2);

        g.translate(0, itemHeight);
    }
}
