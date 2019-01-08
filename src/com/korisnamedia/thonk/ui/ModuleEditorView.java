package com.korisnamedia.thonk.ui;

import com.korisnamedia.thonk.Metronome;
import com.korisnamedia.thonk.ThonkModularApp;
import com.prokmodular.ModuleInfo;
import com.prokmodular.comms.*;
import com.prokmodular.files.ModelExporter;
import com.prokmodular.model.ModelParamListener;
import com.prokmodular.model.ParameterMapping;
import com.prokmodular.model.Preset;
import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.prokmodular.comms.Commands.*;
import static com.prokmodular.comms.Commands.MORPH_Y;
import static org.slf4j.LoggerFactory.getLogger;

public class ModuleEditorView implements ModelParamListener {

    final Logger logger = getLogger(ModuleEditorView.class);
    private final ThonkModularApp app;

    public ModuleSerialConnection moduleSerialConnection;
    private float morphX = -1;
    private float morphY = -1;

    private Metronome metronome;
    private ControlPanel controlPanel;
    private PresetManagerView presetManagerView;

    private ModelExporter modelExporter;
    private Map<Integer, Float> paramCache;

    private boolean uiCreated = false;
    private Map<String, String> moduleState;

    private ModuleInfo currentModule;
    ArrayList<ParameterMapping> parameters;
    public UIControls ui;

    public ModuleEditorView(ThonkModularApp thonkModularApp) {
        app = thonkModularApp;
        paramCache = new HashMap<>();

        parameters = new ArrayList<>();

        moduleState = new HashMap<>();

        moduleState.put(Messages.CPU, "0");
        moduleState.put(Messages.AUDIO_MEMORY, "0");
        moduleState.put(Messages.BLOCK_SIZE,"0");
        moduleState.put(Messages.VERSION, "0");
        moduleState.put(Messages.NAME, "");


    }

    public int getWidth() {
        return app.getWidth();
    }

    public int getHeight() {
        return app.getHeight();
    }

    public void keyPressed(char key) {
        if (key == 32) moduleSerialConnection.sendCommand(new CommandContents(TRIGGER, ""));
    }

    @Override
    public void setModelSize(int numParams) {

    }

    public void close() {
        metronome.stop();
    }

    public void edit(ModuleInfo module) {
        if(ui == null) {
            ui = new UIControls(this, app.cp5);
        }

        if(moduleSerialConnection != null) {
            moduleSerialConnection.removeModelParamListener(this);
        }
        moduleSerialConnection = new ModuleSerialConnection(module.port);
        moduleState.clear();
        moduleSerialConnection.addModelParamListener(this);
        modelExporter = new ModelExporter(moduleSerialConnection);
        metronome = new Metronome(moduleSerialConnection);

        currentModule = module;
        module.ui.createUI(ui, module.getVersion());
        if(currentModule.ui instanceof UIForProcessing) {
            ((UIForProcessing) currentModule.ui).createExtraUI(app.cp5);
        }

    }

    private void createMainUI() {

        logger.debug("Creating main UI");
        uiCreated = true;
        ui.create();

        // Create the appropriate UI
        if (controlPanel == null) {
            controlPanel = new ControlPanel(app.getGraphics(), app.cp5, this);
            controlPanel.createDefaultControls(moduleState);
        }

        if(presetManagerView == null) {
            presetManagerView = new PresetManagerView(app.getGraphics(), app.cp5, this);
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

    public void update() {
        moduleSerialConnection.update();
        controlPanel.showStatus(moduleSerialConnection.connected, moduleState);
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
        app.selectFolder("Choose a folder", "folderChosen");
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

    public File getModelDirectory() {
        File modelDir = new File(app.getDataDirectory(), currentModule.getFilename());
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

    public void setBPM(float value) {
    }

    public File getDataDirectory() {
        return app.getDataDirectory();
    }
}
