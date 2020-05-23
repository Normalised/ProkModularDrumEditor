package com.korisnamedia.thonk;

import com.korisnamedia.thonk.ui.*;
import com.prokmodular.ProkModule;
import com.prokmodular.comms.*;
import com.prokmodular.drums.*;
import com.prokmodular.model.*;
import controlP5.ControlP5;
import controlP5.ControlP5Constants;
import org.slf4j.Logger;
import processing.core.PApplet;
import processing.core.PImage;
import processing.data.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.slf4j.LoggerFactory.*;

public class ThonkModularApp extends PApplet implements ModuleScanStatusListener {

    final Logger logger = getLogger(ThonkModularApp.class);

    private enum AppState {
        SCANNING, MODULE_SELECT, MODULE_EDIT, NO_MODULES_AVAILABLE
    }
    public ControlP5 cp5;

    private ProkModule currentModule;

    private PImage logoTiny;
    private PImage logoBig;

    private Map<String, ModelUI> uis;

    private JSONObject config;
    private File configFile;
    private File prokDir;

//    public static int width = 1450;
//    public static int height = 800;

    private int currentWidth;
    private int currentHeight;

    private ModuleScanner moduleScanner;

    private AppState appState = AppState.SCANNING;

    private ModuleEditorView editorView;
    private ModuleSelectorView selectorView;

    public void settings() {

        File homeDir = new File(System.getProperty("user.home"));
        prokDir = new File(homeDir, "prokDrums");
        configFile = new File(getDataDirectory(), "editorConfig.json");

        logger.debug("settings()");

        moduleScanner = new ModuleScanner();
        moduleScanner.addScanStatusListener(this);

        uis = new HashMap<>();

        addModel(new SnareModel(), new SnareUI());
        addModel(new ClapModel(), new ClapUI());
        addModel(new KickModel(), new KickUI());
        addModel(new HiHatModel(), new HiHatUIForProcessing());
        addModel(new KlonkModel(), new KlonkUI());

        size(width, height);

        loadConfig();
    }

    public void setup() {

        prepareExitHandler();
        logger.debug("Setup");
        logoBig = loadImage("img/Logo_WhiteOnBlack.png");
        logoBig.resize(400,0);
        logoTiny = loadImage("img/Logo_WhiteOnBlack_Tiny.png");

        surface.setTitle("Prok Modular Control");
        surface.setResizable(true);
        surface.setSize(1450, 800);
        surface.setLocation(0,0);

        currentWidth = getWidth();
        currentHeight = getHeight();

        noStroke();
        cp5 = new ControlP5(this);
        cp5.enableShortcuts();
        cp5.setColor(ControlP5Constants.THEME_RETRO);

        selectorView = new ModuleSelectorView(getGraphics(), cp5, this);

        moduleScanner.scan();
    }

    private void loadConfig() {
        if(configFile.exists()) {
            config = loadJSONObject(configFile);
            logger.debug("Loaded config from file");
        } else {
            config = new JSONObject();
            config.setString(ConfigKeys.PRESET_FOLDER,new File(prokDir, "patches").getAbsolutePath());
        }
    }

    public JSONObject getConfig() {
        return config;
    }

    private void addModel(ProkModel model, ModelUI ui) {
        moduleScanner.register(model);
        uis.put(model.getConfig().getName(), ui);
    }

    public void keyPressed() {
        if(editorView != null) {
            editorView.keyPressed(key);
        }
    }

    private void prepareExitHandler() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                try {
                    shutdown();
                    stop();
                } catch (Exception ex) {
                    ex.printStackTrace(); // not much else to do at this point
                }
            }
        }));
    }

    private void shutdown() {
        logger.debug("Shutdown");

        saveJSONObject(config, configFile.getAbsolutePath());
    }

    public void closeEditor() {
        editorView.hideUI();
        currentModule = null;
        appState = AppState.MODULE_SELECT;
        selectorView.show();
    }

    @Override
    public void scanComplete(List<ProkModule> availableModules) {
        if(!availableModules.isEmpty()) {
            showModuleSelect(availableModules);
        } else {
            appState = AppState.NO_MODULES_AVAILABLE;
        }
    }

    private void showModuleSelect(List<ProkModule> modules) {
        logger.debug("Show Module select for " + modules.size() + " modules");
        appState = AppState.MODULE_SELECT;
        selectorView.showAvailableModules(modules);
    }

    public void moduleSelected(ProkModule module) {
        logger.debug("Module Selected " + module.type);

        if(editorView == null) {
            editorView = new ModuleEditorView(this);
        }
        currentModule = module;
        if(currentModule.ui == null) {
            currentModule.ui = uis.get(currentModule.type);
        }
        editorView.edit(currentModule);
        selectorView.hide();
        appState = AppState.MODULE_EDIT;
    }

    public void draw() {

        //logger.debug("Draw");
        background(0, 0, 0);

        // state machine : scanning modules, showing module select, showing param editor
        if(appState == AppState.SCANNING) {
            showSerialStatus();
        } else if(appState == AppState.MODULE_SELECT) {
            selectorView.draw();
        } else if(appState == AppState.MODULE_EDIT) {
            stroke(0xFF777777);
            line(0, height - 30, width, height - 30);
        } else if(appState == AppState.NO_MODULES_AVAILABLE) {
            getGraphics().fill(ControlP5Constants.WHITE);
            showNoModulesInfo();
        }

        if(getWidth() != currentWidth || getHeight() != currentHeight) {
            // Window size changed
            currentWidth = getWidth();
            currentHeight = getHeight();
            if(editorView != null) {
                editorView.resized(currentWidth, currentHeight);
            }
            if(selectorView != null) {
                selectorView.resized(currentWidth, currentHeight);
            }

        }
        drawLogo();
    }

    private void showNoModulesInfo() {
        getGraphics().text("No Modules Found", (getWidth() / 2) - 70, getHeight() - 40);
        getGraphics().text("Please connect your Prok Modules to the computer via USB", (getWidth() / 2) - 190, getHeight() - 20);

    }

    private void showSerialStatus() {
        getGraphics().fill(ControlP5Constants.WHITE);

        if(moduleScanner.isCheckingPorts()) {
            int portCount = moduleScanner.getPortCount();
            //logger.debug("Checking ports " + portCount);
            if(portCount == 0) {
                showNoModulesInfo();
            } else {
                getGraphics().text("Checking " + portCount + " port" + ((portCount != 1) ? "s" : ""), (getWidth() / 2) - 70, getHeight() - 20);
            }

        }
    }

    private void drawLogo() {
        if(appState == AppState.SCANNING || appState == AppState.NO_MODULES_AVAILABLE) {
            image(logoBig, (width - logoBig.width) / 2, (height - logoBig.height) / 2);
        } else {
            image(logoTiny, (width - logoTiny.width) / 2 , height - 26, 50, 21);
        }
    }

    @Override
    public void mousePressed() {

        int logoLeft = (width - logoTiny.width) / 2;
        int logoTop = height - 26;

        if(mouseX >= logoLeft && mouseX <= logoLeft + 50 && mouseY >= logoTop && mouseY <= logoTop + 21) {
            if(appState == AppState.MODULE_EDIT) {
                closeEditor();
            }
        }
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

    public static void main(String[] args) {
        PApplet.main("com.korisnamedia.thonk.ThonkModularApp", args);
    }
}
