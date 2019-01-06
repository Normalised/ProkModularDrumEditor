package com.korisnamedia.thonk;

import com.korisnamedia.thonk.ui.*;
import com.prokmodular.ModuleInfo;
import com.prokmodular.comms.*;
import com.prokmodular.drums.*;
import com.prokmodular.files.ModelExporter;
import com.prokmodular.model.*;
import controlP5.*;
import org.slf4j.Logger;
import processing.core.PApplet;
import processing.core.PImage;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.prokmodular.comms.Commands.*;
import static org.slf4j.LoggerFactory.*;

public class ThonkModularApp extends PApplet implements ModelParamListener {

    final Logger logger = getLogger(ThonkModularApp.class);

    private enum AppState {
        SCANNING, MODULE_SELECT, MODULE_EDIT
    }
    ControlP5 cp5;

    private Map<String, String> moduleState;

    public ModuleSerialConnection moduleSerialConnection;

    private float morphX = -1;
    private float morphY = -1;

    private Metronome metronome;
    private ControlPanel controlPanel;
    private PresetManagerView presetManagerView;

    private ModelExporter modelExporter;

    private ModuleInfo currentModule;

    ArrayList<ParameterMapping> parameters;

    private PImage logoTiny;
    private PImage logoBig;

    //private Map<String, ProkModel> models;
    private Map<String, ModelUI> uis;

    private File prokDir;

    public UIControls ui;
    public static int width = 1450;
    public static int height = 800;
    private String unknownModelName = "";
    private boolean uiCreated = false;
    private Map<Integer, Float> paramCache;

    private ModuleScanner moduleScanner;

    private AppState appState = AppState.SCANNING;

    public void settings() {

        logger.debug("settings()");

        paramCache = new HashMap<>();
        uis = new HashMap<>();

        parameters = new ArrayList<>();

        addModel(new SnareModel(), new SnareUI());
        addModel(new ClapModel(), new ClapUI());
        addModel(new KickModel(), new KickUI());
        addModel(new HiHatModel(), new HiHatUIForProcessing());

        size(width, height);
        moduleState = new HashMap<>();

        moduleState.put(Messages.CPU, "0");
        moduleState.put(Messages.AUDIO_MEMORY, "0");
        moduleState.put(Messages.BLOCK_SIZE,"0");
        moduleState.put(Messages.VERSION, "0");
        moduleState.put(Messages.NAME, "");

        moduleSerialConnection = new ModuleSerialConnection(moduleState);
        moduleScanner = new ModuleScanner();
        moduleSerialConnection.addModelParamListener(this);
        modelExporter = new ModelExporter(moduleSerialConnection);

    }

    private void addModel(ProkModel model, ModelUI ui) {
        moduleScanner.register(model);
        //models.put(model.getConfig().getName(), model);
        uis.put(model.getConfig().getName(), ui);
    }

    public void keyPressed() {
        if (key == 32) moduleSerialConnection.sendCommand(new CommandContents(TRIGGER, ""));
    }

    public void setup() {

        logoBig = loadImage("img/Logo_WhiteOnBlack.png");
        logoTiny = loadImage("img/Logo_WhiteOnBlack_Tiny.png");

        File homeDir = new File(System.getProperty("user.home"));
        prokDir = new File(homeDir, "prokDrums");

        surface.setTitle("Prok Modular Control");
        metronome = new Metronome(moduleSerialConnection);

        noStroke();
        cp5 = new ControlP5(this);
        cp5.setColor(ControlP5Constants.THEME_RETRO);
        ui = new UIControls(this, cp5);

        moduleSerialConnection.init(true);
        moduleScanner.scan();
    }

    public void editorClosed() {
        currentModule = null;
        moduleState.put("name", "");
        metronome.stop();
    }

    public void moduleScanComplete(List<ModuleInfo> availableModules) {
        if(!availableModules.isEmpty()) {
            showModuleSelect(availableModules);
        }
    }

    private void showModuleSelect(List<ModuleInfo> modules) {
        logger.debug("Show Module select for " + modules.size() + " modules");
    }

    public void moduleSelected(ModuleInfo module) {
        logger.debug("Module Selected " + module.type);
        currentModule = module;
        if(currentModule.ui == null) {
            currentModule.ui = uis.get(currentModule.type);
        }
        currentModule.createUI(ui);
        if(currentModule.ui instanceof UIForProcessing) {
            ((UIForProcessing) currentModule.ui).createExtraUI(cp5);
        }
    }

//    private void setModelVersion(String paramValue) {
//        if(currentModel != null) {
//            logger.debug("Set Model Version " + paramValue);
//            currentModel.getConfig().version = parseInt(paramValue);
//        } else {
//            logger.debug("CURRENT MODEL IS NULL");
//        }
//    }

    private void createMainUI() {

        logger.debug("Creating main UI");
        uiCreated = true;
        ui.create();

        // Create the appropriate UI
        if (controlPanel == null) {
            controlPanel = new ControlPanel(getGraphics(), cp5, this);
            controlPanel.createDefaultControls(moduleState);
        }

        if(presetManagerView == null) {
            presetManagerView = new PresetManagerView(getGraphics(), cp5, this);
            presetManagerView.createUI();
        }
        presetManagerView.setCurrentModel(currentModule.model, getModelDirectory());
        metronome.start();

        if(!paramCache.isEmpty()) {
            for(Map.Entry<Integer, Float> item : paramCache.entrySet()) {
                setCurrentParam(new ParamMessage(item.getKey(), item.getValue()));
            }

            paramCache.clear();
        }
    }

    public void clear() {
        moduleSerialConnection.sendCommand(new CommandContents(CLEAR, ""));
    }

    public void draw() {

        background(0, 0, 0);

        moduleSerialConnection.update();

        // state machine : scanning modules, showing module select, showing param editor
        if(appState == AppState.SCANNING) {
            showSerialStatus();
        } else if(appState == AppState.MODULE_SELECT) {

        } else if(appState == AppState.MODULE_EDIT) {
            controlPanel.showStatus(moduleSerialConnection.connected, moduleState);
            stroke(0xFF777777);
            line(0, height - 30, width, height - 30);
        }

        drawLogo();
    }

    private void showSerialStatus() {
        getGraphics().fill(ControlP5Constants.WHITE);

        if(moduleSerialConnection.isCheckingPorts()) {
            int portCount = Serial.list().length;
            getGraphics().text("Checking " + portCount + " port" + ((portCount != 1) ? "s" : ""), (getWidth() / 2) - 70, getHeight() - 20);
        }
    }

    private void drawLogo() {
        if(currentModule != null) {
            // 584, 262
            image(logoTiny, (width / 2) - 29, height - 26, 50, 21);
        } else {
            image(logoBig, (width - logoBig.width) / 2, (height - logoBig.height) / 2);
        }

    }

    public void applyPreset(Preset p) {
        logger.debug("Apply preset " + p.config.hello + ". Version " + p.config.version + " with " + p.params.size() + " params");

        if(p.config.version > currentModule.getVersion()) {
            logger.debug("Preset version is newer than firmware");
            return;
        }

        if(p.params.size() != parameters.size()) {
            logger.debug("Preset has wrong number of params. Expected " + parameters.size());
        }

        for(int i=0;i<p.params.size();i++) {
            ParameterMapping mapping = parameters.get(i);
            ui.setControlValue(i, mapping.fromModule(p.params.get(i)));
        }
    }

    public void setBPM(float bpm) {

    }

    public void useMetronome(boolean on) {
        metronome.on(on);
    }

    public void setParam(int modelIndex, ParamMessage msg) {
        logger.debug("Set Param. " + modelIndex + " : " + msg.id + " -> " + msg.value);
    }

    public void generateHeader() {
        modelExporter.generateHeader(currentModule.model);
    }

    public void saveModelsLocally() {
        selectFolder("Choose a folder", "folderChosen");
        //modelExporter.saveLocally(currentModel);
    }

    public void folderChosen(File selectedFolder) {
        modelExporter.saveLocally(currentModule.model, selectedFolder);
    }

    public void getCurrentParams() {
        moduleSerialConnection.sendCommand(new CommandContents(Commands.SEND_PARAMS, Commands.INDEX_FOR_CURRENT_MODEL));
    }

    public void saveModel(int index) {
        moduleSerialConnection.sendCommand(new CommandContents(Commands.SAVE, String.valueOf(index)));
    }

    public void selectModel(int index) {
        moduleSerialConnection.sendCommand(new CommandContents(Commands.SELECT_MODEL, String.valueOf(index)));
    }

    public void clearQuad(int index) {
        moduleSerialConnection.sendCommand(new CommandContents(Commands.CLEAR_QUAD, String.valueOf(index)));
    }

    public void setExclusive(boolean on) {
        moduleSerialConnection.sendCommand(new CommandContents(Commands.EXCLUSIVE, on ? "1" : "0"));
    }

    public void setModelSize(int numParams) {
        logger.debug("Set Model Size " + numParams);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public File getDataDirectory() {
        return prokDir;
    }

    public File getModelDirectory() {
        File modelDir = new File(getDataDirectory(), currentModule.getFilename());
        if(!modelDir.exists()) {
            modelDir.mkdirs();
        }
        return modelDir;
    }

    public Preset getPreset() {
        List<Float> params = new ArrayList<>(parameters.size());
        for(int i=0;i<parameters.size();i++) {
            params.add(parameters.get(i).toModule(ui.getControlValue(i)));
        }
        return new Preset(currentModule.model.getConfig(), params);
    }

    public void setMorph(float newX, float newY) {
        if (newX != morphX) {
            morphX = newX;
            moduleSerialConnection.sendCommand(new CommandContents(MORPH_X, Float.toString(morphX)));
        }
        if (newY != morphY) {
            morphY = newY;
            moduleSerialConnection.sendCommand(new CommandContents(MORPH_Y, Float.toString(morphY)));
        }
    }

    public int getCurrentParamId() {
        return parameters.size();
    }

    public void addParameter(ParameterMapping mapping) {
        parameters.add(mapping);
    }

    public void setCurrentParam(ParamMessage msg) {
        if (parameters.size() == 0) {
            if(!uiCreated) {
                // cache params until ui is created
                paramCache.put(msg.id, msg.value);
            } else {
                logger.debug("Parameters are empty, cant set current param for ID " + msg.id + " to " + msg.value);
            }
            return;
        }
        ParameterMapping mapping = parameters.get(msg.id);
        ui.setCurrentParam(msg.id, mapping.fromModule(msg.value));
    }

    public void handleControlEvent(int paramID, float val) {
        if (paramID >= parameters.size()) return;
        ParameterMapping mapping = parameters.get(paramID);
        moduleSerialConnection.sendCurrentParam(new ParamMessage(paramID, mapping.toModule(val)));
    }


    public static void main(String[] args) {
        PApplet.main("com.korisnamedia.thonk.ThonkModularApp", args);
    }
}
