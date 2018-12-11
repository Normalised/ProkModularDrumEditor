package com.korisnamedia.thonk;

import com.korisnamedia.thonk.ui.HiHatUIForProcessing;
import com.korisnamedia.thonk.ui.UIForProcessing;
import com.prokmodular.comms.Serial;
import com.prokmodular.drums.*;
import com.prokmodular.files.ModelExporter;
import com.korisnamedia.thonk.ui.UIControls;
import com.prokmodular.comms.Commands;
import com.prokmodular.model.*;
import com.prokmodular.comms.SerialCommunicator;
import com.prokmodular.comms.SerialCommunicatorListener;
import controlP5.*;
import processing.core.PApplet;
import processing.core.PImage;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.prokmodular.comms.Commands.*;

public class ThonkModularApp extends PApplet implements SerialCommunicatorListener, ModelParamListener {

    ControlP5 cp5;

    private Map<String, String> moduleState;
    public SerialCommunicator serialCommunicator;

    private float morphX = -1;
    private float morphY = -1;

    private Metronome metronome;
    private ControlPanel controlPanel;
    private PresetManagerView presetManagerView;

    private ModelExporter modelExporter;

    private ProkModel currentModel;
    private ModelUI currentUI;

    ArrayList<ParameterMapping> parameters;
    private PImage logoTiny;

    private PImage logoBig;
    private Map<String, ProkModel> models;

    private Map<String, ModelUI> uis;

    private File prokDir;

    public UIControls ui;
    public static int width = 1450;
    public static int height = 800;

    public void settings() {

        models = new HashMap<>();
        uis = new HashMap<>();

        parameters = new ArrayList<>();

        addModel(new SnareModel(), new SnareUI());
        addModel(new ClapModel(), new ClapUI());
        addModel(new KickModel(), new KickUI());
        addModel(new HiHatModel(), new HiHatUIForProcessing());

        size(width, height);
        moduleState = new HashMap<>();
        moduleState.put("cpu", "0");
        moduleState.put("audiomem", "0");
        moduleState.put("version", "0");
        moduleState.put("name", "");
        serialCommunicator = new SerialCommunicator(moduleState);
        serialCommunicator.addSerialCommsListener(this);
        serialCommunicator.addModelParamListener(this);
        modelExporter = new ModelExporter(serialCommunicator);

    }

    private void addModel(ProkModel model, ModelUI ui) {
        models.put(model.getConfig().getName(), model);
        uis.put(model.getConfig().getName(), ui);
    }

    public void keyPressed() {
        if (key == 32) serialCommunicator.sendCommand(TRIGGER, "");
    }

    public void setup() {

        logoBig = loadImage("img/Logo_WhiteOnBlack.png");
        logoTiny = loadImage("img/Logo_WhiteOnBlack_Tiny.png");

        File homeDir = new File(System.getProperty("user.home"));
        prokDir = new File(homeDir, "prokDrums");

        surface.setTitle("Prok Modular Control");
        metronome = new Metronome(serialCommunicator);

        noStroke();
        cp5 = new ControlP5(this);
        cp5.setColor(ControlP5Constants.THEME_RETRO);
        ui = new UIControls(this, cp5);

        serialCommunicator.init();
    }

    public void serialConnected(String helloType) {
        println("Serial Connected " + helloType);
        ui.create();

        // Create the appropriate UI
        if (controlPanel == null) {
            controlPanel = new ControlPanel(getGraphics(), cp5, this);
            controlPanel.createDefaultControls();
        }

        currentModel = models.get(helloType);
        currentUI = uis.get(helloType);
        createUIForModel();

        if(presetManagerView == null) {
            presetManagerView = new PresetManagerView(getGraphics(), cp5, this);
            presetManagerView.createUI();
        }
        presetManagerView.setCurrentModel(currentModel, getModelDirectory());
        moduleState.put("name", currentModel.getConfig().getName());
        metronome.start();
    }

    @Override
    public void serialDisconnected() {
        currentModel = null;
        moduleState.put("name", "");
        metronome.stop();
    }

    private void createUIForModel() {
        currentUI.createUI(ui);
        if(currentUI instanceof UIForProcessing) {
            ((UIForProcessing) currentUI).createExtraUI(cp5);
        }
    }

    public void onData(String paramName, String paramValue) {
        if (paramName.equalsIgnoreCase(Commands.VERSION)) {

            setModelVersion(paramValue);

        } else if(paramName.equalsIgnoreCase(Commands.FIRMWARE_VERSION)) {
            println("Firmware Version " + paramValue);
        } else if(paramName.equalsIgnoreCase(Commands.HAS_SD_CARD)) {
            println("Has SD Card " + paramValue);
        }
    }

    private void setModelVersion(String paramValue) {
        if(currentModel != null) {
            currentModel.getConfig().version = parseInt(paramValue);
        } else {
            println("CURRENT MODEL IS NULL");
        }
    }

    public void clear() {
        serialCommunicator.sendCommand(CLEAR, "");
    }

    public void draw() {

        background(0, 0, 0);

        serialCommunicator.update();

        drawLogo();

        if (serialCommunicator.connected) {
            controlPanel.showStatus(serialCommunicator.connected, moduleState);
            stroke(0xFF777777);
            line(0, height - 30, width, height - 30);
        } else {
            showSerialStatus();
        }
    }

    private void showSerialStatus() {
        getGraphics().fill(ControlP5Constants.WHITE);

        int portCount = Serial.list().length;
        getGraphics().text("Waiting Connection, found " + portCount + " port" + ((portCount != 1) ? "s" : ""), (getWidth() / 2) - 70, getHeight() - 20);
    }

    private void drawLogo() {
        if(serialCommunicator.connected) {
            // 584, 262
            image(logoTiny, (width / 2) - 29, height - 26, 50, 21);
        } else {
            image(logoBig, (width - logoBig.width) / 2, (height - logoBig.height) / 2);
        }

    }

    public void applyPreset(Preset p) {
        cp5.println("Apply preset " + p.config.hello + ". Version " + p.config.version + " with " + p.params.size() + " params");

        if(p.params.size() != parameters.size()) {
            cp5.println("Preset has wrong number of params. Expected " + parameters.size());
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

    public void setParam(int modelIndex, int paramID, float paramValue) {
        println("Set Param. " + modelIndex + " : " + paramID + " -> " + paramValue);
    }

    public void generateHeader() {
        modelExporter.generateHeader(currentModel);
    }

    public void saveModelsLocally() {
        selectFolder("Choose a folder", "folderChosen");
        //modelExporter.saveLocally(currentModel);
    }

    public void folderChosen(File selectedFolder) {
        modelExporter.saveLocally(currentModel, selectedFolder);
    }

    public void getCurrentParams() {
        serialCommunicator.sendCommand(Commands.SEND_PARAMS, Commands.INDEX_FOR_CURRENT_MODEL);
    }

    public void saveModel(int index) {
        serialCommunicator.sendCommand(Commands.SAVE, String.valueOf(index));
    }

    public void selectModel(int index) {
        serialCommunicator.sendCommand(Commands.SELECT_MODEL, String.valueOf(index));
    }

    public void clearQuad(int index) {
        serialCommunicator.sendCommand(Commands.CLEAR_QUAD, String.valueOf(index));
    }

    public void setExclusive(boolean on) {
        serialCommunicator.sendCommand(Commands.EXCLUSIVE, on ? "1" : "0");
    }

    public void setModelSize(int numParams) {
        println("Set Model Size " + numParams);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public static void main(String[] args) {
        PApplet.main("com.korisnamedia.thonk.ThonkModularApp", args);
    }

    public File getDataDirectory() {
        return prokDir;
    }

    public File getModelDirectory() {
        File modelDir = new File(getDataDirectory(), currentModel.getConfig().filename);
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
        return new Preset(currentModel.getConfig(), params);
    }

    public void setMorph(float newX, float newY) {
        if (newX != morphX) {
            morphX = newX;
            serialCommunicator.sendCommand(MORPH_X, Float.toString(morphX));
        }
        if (newY != morphY) {
            morphY = newY;
            serialCommunicator.sendCommand(MORPH_Y, Float.toString(morphY));
        }
    }

    public int getCurrentParamId() {
        return parameters.size();
    }

    public void addParameter(ParameterMapping mapping) {
        parameters.add(mapping);
    }

    public void setCurrentParam(int paramID, float val) {
        if (parameters.size() == 0) {
            cp5.println("Parameters are empty, cant set current param for ID " + paramID + " to " + val);
            return;
        }
        ParameterMapping mapping = parameters.get(paramID);
        ui.setCurrentParam(paramID, mapping.fromModule(val));
    }

    public void handleControlEvent(int paramID, float val) {
        if (paramID >= parameters.size()) return;
        ParameterMapping mapping = parameters.get(paramID);
        serialCommunicator.sendCurrentParam(paramID, mapping.toModule(val));
    }
}
