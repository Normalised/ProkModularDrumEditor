package com.korisnamedia.thonk;

import com.korisnamedia.thonk.ui.*;
import com.prokmodular.ModuleInfo;
import com.prokmodular.comms.*;
import com.prokmodular.drums.*;
import com.prokmodular.model.*;
import controlP5.*;
import org.slf4j.Logger;
import processing.core.PApplet;
import processing.core.PImage;

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


    private ModuleInfo currentModule;


    private PImage logoTiny;
    private PImage logoBig;

    //private Map<String, ProkModel> models;
    private Map<String, ModelUI> uis;

    private File prokDir;

    public static int width = 1450;
    public static int height = 800;
    private String unknownModelName = "";

    private ModuleScanner moduleScanner;

    private AppState appState = AppState.SCANNING;

    private ModuleEditorView editorView;

    public void settings() {

        logger.debug("settings()");
        moduleScanner = new ModuleScanner();
        moduleScanner.addScanStatusListener(this);

        uis = new HashMap<>();

        addModel(new SnareModel(), new SnareUI());
        addModel(new ClapModel(), new ClapUI());
        addModel(new KickModel(), new KickUI());
        addModel(new HiHatModel(), new HiHatUIForProcessing());

        size(width, height);


    }

    private void addModel(ProkModel model, ModelUI ui) {
        moduleScanner.register(model);
        uis.put(model.getConfig().getName(), ui);
    }

    public void keyPressed() {
        editorView.keyPressed(key);
    }

    public void setup() {

        logger.debug("Setup");
        logoBig = loadImage("img/Logo_WhiteOnBlack.png");
        logoTiny = loadImage("img/Logo_WhiteOnBlack_Tiny.png");

        File homeDir = new File(System.getProperty("user.home"));
        prokDir = new File(homeDir, "prokDrums");

        surface.setTitle("Prok Modular Control");

        noStroke();
        cp5 = new ControlP5(this);
        cp5.setColor(ControlP5Constants.THEME_RETRO);

        moduleScanner.scan();
    }

    public void editorClosed() {
        editorView.close();
        currentModule = null;
    }

    @Override
    public void scanComplete(List<ModuleInfo> availableModules) {
        if(!availableModules.isEmpty()) {
            showModuleSelect(availableModules);
        } else {
            appState = AppState.NO_MODULES_AVAILABLE;
        }
    }

    private void showModuleSelect(List<ModuleInfo> modules) {
        logger.debug("Show Module select for " + modules.size() + " modules");
    }

    public void moduleSelected(ModuleInfo module) {
        logger.debug("Module Selected " + module.type);

        if(editorView == null) {
            editorView = new ModuleEditorView(this);
        }
        currentModule = module;
        if(currentModule.ui == null) {
            currentModule.ui = uis.get(currentModule.type);
        }
        editorView.edit(currentModule);

    }

    public void draw() {

        //logger.debug("Draw");
        background(0, 0, 0);

        // state machine : scanning modules, showing module select, showing param editor
        if(appState == AppState.SCANNING) {
            showSerialStatus();
        } else if(appState == AppState.MODULE_SELECT) {

        } else if(appState == AppState.MODULE_EDIT) {
            editorView.update();
            stroke(0xFF777777);
            line(0, height - 30, width, height - 30);
        } else if(appState == AppState.NO_MODULES_AVAILABLE) {
            getGraphics().fill(ControlP5Constants.WHITE);
            getGraphics().text("No Modules Found", (getWidth() / 2) - 50, getHeight() - 20);
        }

        drawLogo();
    }

    private void showSerialStatus() {
        getGraphics().fill(ControlP5Constants.WHITE);

        if(moduleScanner.isCheckingPorts()) {
            int portCount = moduleScanner.getPortCount();
            //logger.debug("Checking ports " + portCount);
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
