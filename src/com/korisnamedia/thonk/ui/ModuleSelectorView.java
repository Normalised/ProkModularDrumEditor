package com.korisnamedia.thonk.ui;

import com.korisnamedia.thonk.ConfigKeys;
import com.korisnamedia.thonk.ThonkModularApp;
import com.prokmodular.ProkModule;
import com.prokmodular.comms.ParamMessage;
import com.prokmodular.model.Preset;
import com.prokmodular.model.PresetManager;
import controlP5.ControlP5;
import controlP5.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.data.JSONArray;
import processing.data.JSONObject;

import java.io.File;
import java.util.*;


public class ModuleSelectorView {
    final Logger logger = LoggerFactory.getLogger(ModuleSelectorView.class);
    private final ControlP5 cp5;
    private final PGraphics graphics;
    private final ThonkModularApp app;

    private PImage bdPanel;
    private PImage snPanel;
    private PImage cpPanel;
    private PImage hhPanel;

    private int panelWidth = 60;
    private int panelHeight = -1;
    private int spacing = 10;
    private int top = 150;
    private List<ProkModule> modules;
    private HashMap<String,ControlPanel> panels;

    private ArrayList<String> panelLayout;

    private PresetManager presetManager;
    private BankLoader bankLoader;

    public ModuleSelectorView(PGraphics graphics, ControlP5 cp5, ThonkModularApp thonkModularApp) {
        this.cp5 = cp5;
        this.graphics = graphics;
        app = thonkModularApp;
        bdPanel = app.loadImage("img/Prok_Drums_BD.jpg");
        snPanel = app.loadImage("img/Prok_Drums_SN.jpg");
        cpPanel = app.loadImage("img/Prok_Drums_CP.jpg");
        hhPanel = app.loadImage("img/Prok_Drums_HH.jpg");

        float ratio = (float) panelWidth / (float) bdPanel.width;
        panelHeight = Math.round((ratio * (float)bdPanel.height));

        presetManager = new PresetManager();

        panelLayout = new ArrayList<>();

        if(app.getConfig().hasKey(ConfigKeys.MODULE_LAYOUT)) {
            JSONArray layoutConfig = app.getConfig().getJSONArray(ConfigKeys.MODULE_LAYOUT);

            for(int i=0;i<layoutConfig.size();i++) {
                String key = layoutConfig.getString(i);
                panelLayout.add(key);
            }
        }
        bankLoader = new BankLoader(app);
    }

    public void showAvailableModules(List<ProkModule> modulesToUse) {
        logger.debug("Show Available Modules " + modulesToUse.size());

        panels = new HashMap<>();
        modules = modulesToUse;

        for(ProkModule module : modulesToUse) {
            ControlPanel panel = createPanelForModule(module);
            panels.put(module.getConnectionKey(), panel);
        }
        updateLayout();
    }

    private ControlPanel createPanelForModule(ProkModule module) {

        logger.debug("Create Panel for Module " + module.type);

        ControlPanel panel = new ControlPanel(cp5, module.type);

        panel.setPanelImage(getImageForModule(module.type));
        panel.setVerticalLayout(true);
        panel.setModule(module);
        panel.setSize(panelWidth, panelHeight + 150);
        panel.onSDClicked(theEvent -> loadBankIntoModule(module));
        panel.onMoveLeftClicked(theEvent -> movePanelLeft(panel));
        panel.onMoveRightClicked(theEvent -> movePanelRight(panel));
        panel.onPanelClicked(theEvent ->  {
            if(cp5.isControlDown()) {
                //logger.debug("Reboot");
                //module.reboot();
                module.readEeprom(0);
            } else {
                app.moduleSelected(module);
            }
        });
        panel.onTriggerClicked(theEvent -> module.trigger());
        panel.onCheckSD(theEvent -> module.quickOpenSD());
        return panel;
    }

    private void loadBankIntoModule(ProkModule module) {
        bankLoader.load(module);
    }

    private void movePanelRight(ControlPanel panel) {
        logger.debug("Move Panel Right " + panel.getModule().type);

        String thisPanelKey = panel.getModule().getConnectionKey();
        int thisPanelIndex = panelLayout.indexOf(thisPanelKey);
        int nextPanelIndex = thisPanelIndex + 1;
        Collections.swap(panelLayout, thisPanelIndex, nextPanelIndex);
        updateLayout();
    }

    private void movePanelLeft(ControlPanel panel) {
        logger.debug("Move Panel Left " + panel.getModule().type);

        String thisPanelKey = panel.getModule().getConnectionKey();
        int thisPanelIndex = panelLayout.indexOf(thisPanelKey);
        int prevPanelIndex = thisPanelIndex - 1;
        Collections.swap(panelLayout, thisPanelIndex, prevPanelIndex);
        updateLayout();
    }

    private void updateLayout() {
        logger.debug("Update Layout");
        int numPanels = panels.size();

        boolean clearLayout = false;
        // New or removed modules?
        if(panelLayout.size() > 0 && modules.size() != panelLayout.size()) {
            clearLayout = true;
        }

        for(ProkModule module : modules) {
            if(panelLayout.indexOf(module.getConnectionKey()) == -1) clearLayout = true;
        }

        if(clearLayout) {
            logger.debug("Clearing layout");
            panelLayout.clear();
            for(ProkModule module : modules) {
                panelLayout.add(module.getConnectionKey());
            }
        }

        int width = app.getWidth();
        int totalWidth = (numPanels * panelWidth) + ((numPanels - 1) * spacing);
        int leftBorder = (width - totalWidth) / 2;

        int displayIndex;
        for(Map.Entry<String, ControlPanel> entry : panels.entrySet()) {
            ControlPanel p = entry.getValue();
            displayIndex = panelLayout.indexOf(p.getModule().getConnectionKey());
            p.setPosition(leftBorder + ((panelWidth + spacing) * displayIndex), top);
            p.canMoveRight = displayIndex < numPanels - 1;
            p.canMoveLeft = displayIndex > 0;
        }

        JSONObject config = app.getConfig();

        JSONArray layoutConfig = new JSONArray();
        int index = 0;
        for(String key : panelLayout) {
            layoutConfig.setString(index++, key);
        }

        config.setJSONArray(ConfigKeys.MODULE_LAYOUT, layoutConfig);
        logger.debug("End Update Layout");
    }

    public void draw() {

    }

    private PImage getImageForModule(String key) {
        switch(key) {
            case "kick":
                return bdPanel;
            case "snare":
                return snPanel;
            case "hihat":
                return hhPanel;
            case "clap":
                return cpPanel;
            default:
                return bdPanel;
        }
    }

    public void hide() {
        for(ControlPanel panel : panels.values()) {
            panel.hide();
        }
    }

    public void show() {
        for(ControlPanel panel : panels.values()) {
            panel.show();
        }
    }

    public void resized(int currentWidth, int currentHeight) {
        int numPanels = panels.size();
        int width = currentWidth;
        int totalWidth = (numPanels * panelWidth) + ((numPanels - 1) * spacing);
        int leftBorder = (width - totalWidth) / 2;

        int displayIndex;
        for(Map.Entry<String, ControlPanel> entry : panels.entrySet()) {
            ControlPanel p = entry.getValue();
            displayIndex = panelLayout.indexOf(p.getModule().getConnectionKey());
            p.setPosition(leftBorder + ((panelWidth + spacing) * displayIndex), top);
        }

    }
}
