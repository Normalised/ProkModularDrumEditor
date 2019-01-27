package com.korisnamedia.thonk.ui;

import com.korisnamedia.thonk.Metronome;
import com.korisnamedia.thonk.ThonkModularApp;
import com.prokmodular.ProkModule;
import com.prokmodular.comms.*;
import com.prokmodular.files.ModelExporter;
import com.prokmodular.model.ModelParamListener;
import com.prokmodular.model.ParameterMapping;
import com.prokmodular.model.Preset;
import controlP5.Button;
import controlP5.Slider;
import controlP5.Textlabel;
import controlP5.Toggle;
import org.slf4j.Logger;
import processing.data.JSONObject;

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
    public static final int METRONOME_ID = 101;
    private final ThonkModularApp app;
    private final BankLoader bankLoader;

    private float morphX = -1;
    private float morphY = -1;

    private Metronome metronome;
    private ControlPanel controlPanel;
    private MorphAndStorage morphAndStorage;
    private PresetManagerUI presetManagerUI;

    private ModelExporter modelExporter;
    private Map<Integer, Float> paramCache;

    private ProkModule currentModule;
    ArrayList<ParameterMapping> parameters;
    public UIControls ui;
    private boolean devMode = false;
    private Button generateHeaderButton;
    private Button saveBankButton;
    private Button loadBankButton;
    private Toggle exclusiveToggle;
    private Toggle metroToggle;
    private Slider metroSlider;
    private Textlabel ignoreCVLabel;

    private boolean settingUpModule = false;

    public ModuleEditorView(ThonkModularApp thonkModularApp) {
        app = thonkModularApp;
        bankLoader = new BankLoader(app);
        paramCache = new HashMap<>();

        parameters = new ArrayList<>();

        ui = new UIControls(this, app.cp5);
        ui.setPosition(450,10);
        ui.createNoteControls();

        modelExporter = new ModelExporter();
        metronome = new Metronome();

        controlPanel = new ControlPanel(app.cp5, "editorControlPanel");
        controlPanel.setSize(300,24);
        controlPanel.setPosition(getWidth() - 300,getHeight() - 24);

        morphAndStorage = new MorphAndStorage(app.cp5, this);
        morphAndStorage.setPosition(220,21);
        morphAndStorage.createUI();

        presetManagerUI = new PresetManagerUI(app.cp5, this);
        presetManagerUI.setPosition(10,10);
        presetManagerUI.createUI();

        createToolsButtons();
        createMetronomeControls();
    }

    public void edit(ProkModule module) {

        settingUpModule = true;

        parameters.clear();

        if(currentModule != null) {
            currentModule.removeParamListener(this);
        }
        currentModule = module;
        currentModule.addParamListener(this);

        ui.setModule(module);

        metronome.setModule(currentModule);
        modelExporter.setModule(currentModule);

        presetManagerUI.setModule(currentModule);
        controlPanel.setModule(module);
        morphAndStorage.setModule(module);

        if(!paramCache.isEmpty()) {
            logger.debug("Param Cache has data.");
            for(Map.Entry<Integer, Float> item : paramCache.entrySet()) {
                setCurrentParam(new ParamMessage(item.getKey(), item.getValue()));
            }

            paramCache.clear();
        }

        controlPanel.show();
        morphAndStorage.show();
        presetManagerUI.show();
        if(devMode){
            generateHeaderButton.show();
        }
        saveBankButton.show();
        loadBankButton.show();
        exclusiveToggle.show();
        ignoreCVLabel.show();
        metroToggle.show();
        metroSlider.show();
        ui.show();

        getCurrentParams();
        settingUpModule = false;
    }

    public void close() {
        //metronome.stop();
        controlPanel.hide();
        morphAndStorage.hide();
        presetManagerUI.hide();
        if(devMode) {
            generateHeaderButton.hide();
        }
        saveBankButton.hide();
        loadBankButton.hide();
        exclusiveToggle.hide();
        ignoreCVLabel.hide();

        metroToggle.hide();
        metroSlider.hide();
        metroToggle.setValue(false);
        //metronome.on(false);
        ui.hide();
        ui.clear();
    }

    public int getWidth() {
        return app.getWidth();
    }

    public int getHeight() {
        return app.getHeight();
    }

    public void keyPressed(char key) {
        if (key == 32) {
            logger.debug("Sending trigger to " + currentModule.type);
            currentModule.trigger();
        }
    }

    @Override
    public void setModelSize(int numParams) {

    }

    public void clear() {
        currentModule.clearPatches();
    }

    private void createToolsButtons() {
        int buttonsX = 220;

        loadBankButton = app.cp5.addButton("Load Bank").setPosition(buttonsX,10).setSize(95,20).onRelease(theEvent -> {
            bankLoader.load(currentModule);
        });

        saveBankButton = app.cp5.addButton("Save Bank").setPosition(buttonsX + 105,10).setSize(95,20).onRelease(theEvent -> {
            saveModelsLocally();
        });

        if(devMode) {
//            generateHeaderButton = app.cp5.addButton("Generate Header").setPosition(buttonsX, 10).setSize(95, 20).onRelease(theEvent -> {
//                generateHeader();
//            });

        }


        exclusiveToggle = app.cp5.addToggle("Exclusive")
                .setPosition(300, app.getHeight() - 24)
                .setSize(20,20)
                .setValue(false)
                .onChange(theEvent -> {
                    boolean on = theEvent.getController().getValue() > 0;
                    setExclusive(on);
                });

        ignoreCVLabel = app.cp5.addTextlabel( "IgnoreCV", "Ignore CV", 322, app.getHeight() - 19);
    }

    private void loadBank() {

    }

    private void createMetronomeControls() {
        int y = app.getHeight() - 24;

        metroToggle = app.cp5.addToggle("Metro")
                .setPosition(10, y)
                .setSize(20,20)
                .setLabelVisible(false)
                .onChange(theEvent -> {
                    boolean on = theEvent.getController().getValue() > 0;
                    useMetronome(on);
                }).setValue(false);

        metroSlider = ui.addLocalSlider("Tempo", 10, 400, METRONOME_ID + 1).setPosition(32, y).setSize(178,20);
        metroSlider.setValue(120.0f);
        metroSlider.onChange(theEvent -> {
           metronome.setBPM(metroSlider.getValue());
        });
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
        currentModule.getCurrentParams();
    }

    public void saveModel(int index) {
        currentModule.saveModel(index);
    }

    public void selectModel(int index) {
        currentModule.selectModel(index);
    }

    public void clearQuad(int index) {
        currentModule.clearQuad(index);
    }

    public void setExclusive(boolean on) {
        currentModule.ignoreCV(on);
    }

    public Preset getPreset() {
        List<Float> params = new ArrayList<>(parameters.size());
        for(int i=0;i<parameters.size();i++) {
            params.add(parameters.get(i).toModule(ui.getControlValue(i)));
        }
        return new Preset(currentModule.model.getConfig(), params);
    }

    public void setMorph(float newX, float newY) {
        if(currentModule == null) return;

        if (newX != morphX) {
            morphX = newX;
            currentModule.morphX(morphX);

        }
        if (newY != morphY) {
            morphY = newY;
            currentModule.morphY(morphY);
        }
    }

    public int getCurrentParamId() {
        return parameters.size();
    }

    public void addParameter(ParameterMapping mapping) {
        //logger.debug("Add Parameter Mapping " + parameters.size());
        parameters.add(mapping);
    }

    public void setCurrentParam(ParamMessage msg) {
        if (parameters.size() == 0) {
            paramCache.put(msg.id, msg.value);
            return;
        }
        ParameterMapping mapping = parameters.get(msg.id);
        ui.setCurrentParam(msg.id, mapping.fromModule(msg.value));
    }

    public void handleControlEvent(int paramID, float val) {
        if(settingUpModule) return;

//        logger.debug("Handle Control Event " + paramID + " : " + val + " : " + parameters.size());
        if (paramID >= parameters.size()) return;
        ParameterMapping mapping = parameters.get(paramID);
        currentModule.setParam(new ParamMessage(paramID, mapping.toModule(val)));

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

    public JSONObject getConfig() {
        return app.getConfig();
    }
}
