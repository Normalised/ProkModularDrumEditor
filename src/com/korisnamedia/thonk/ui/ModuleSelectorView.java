package com.korisnamedia.thonk.ui;

import com.korisnamedia.thonk.ConfigKeys;
import com.korisnamedia.thonk.ThonkModularApp;
import com.prokmodular.ModuleInfo;
import com.prokmodular.comms.Commands;
import com.prokmodular.comms.Serial;
import controlP5.ControlP5;
import controlP5.ControlWindow;
import controlP5.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.data.JSONArray;
import processing.data.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
    private List<ModuleInfo> modules;
    private List<ControlPanel> panels;

    private HashMap<String, Integer> panelLayout;

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

        panelLayout = new HashMap<>();

        if(app.getConfig().hasKey(ConfigKeys.MODULE_LAYOUT)) {
            JSONArray layoutConfig = app.getConfig().getJSONArray(ConfigKeys.MODULE_LAYOUT);

            for(int i=0;i<layoutConfig.size();i++) {
                String key = layoutConfig.getString(i);
                panelLayout.put(key, i);
            }
        }
    }

    public void showAvailableModules(List<ModuleInfo> modulesToUse) {
        logger.debug("Show Available Modules " + modulesToUse.size());

        panels = new ArrayList<>();
        int index = 0;
        int numModules = modulesToUse.size();
        int displayIndex = 0;
        for(ModuleInfo info : modulesToUse) {
            ControlPanel panel = new ControlPanel(cp5, info.type);
            String moduleKey = info.getConnectionKey();
            if(panelLayout.containsKey(moduleKey)) {
                logger.debug("Layout has panel " + moduleKey + " : " + panelLayout.get(moduleKey));
                displayIndex = panelLayout.get(moduleKey);
            } else {
                logger.debug("No layout for " + moduleKey);
                panelLayout.put(moduleKey, index);
                displayIndex = index;
            }

            panel.canMoveLeft = displayIndex > 0;
            panel.canMoveRight = displayIndex < numModules - 1;

            panel.setPanelImage(getImageForModule(info.type));
            panel.setVerticalLayout(true);
            panel.setModule(info);
            panel.setSize(panelWidth, panelHeight + 150);
            panel.onRelease(theEvent -> {
                Pointer p = panel.getPointer();
                logger.debug(p.toString());
                int x = p.x();
                int y = p.y();
                if(y <= 20) {
                    if(x < panel.getWidth() / 2 && panel.canMoveLeft) {
                        movePanelLeft(panel);
                    } else if(x > panel.getWidth() / 2 && panel.canMoveRight) {
                        movePanelRight(panel);
                    }
                    return;
                }
                if(x >= 25 && x <= 55 && y >= 235 && y <= 265) {
                    logger.debug("Sending trigger to " + info.type);
                    info.connection.sendCommandWithNoData(Commands.TRIGGER);
                } else {
                    app.moduleSelected(info);
                }
            });
            panels.add(panel);
            index++;
        }
        updateLayout();
        this.modules = modulesToUse;
    }

    private void movePanelRight(ControlPanel panel) {
        logger.debug("Move Panel Right " + panel.getModule().type);

        String thisPanelKey = panel.getModule().getConnectionKey();
        int thisPanelIndex = panelLayout.get(thisPanelKey);

        for(ControlPanel p : panels) {
            String key = p.getModule().getConnectionKey();
            int displayIndex = panelLayout.get(key);
            if(displayIndex == thisPanelIndex + 1) {
                panelLayout.put(key, thisPanelIndex);
                break;
            }
        }
        panelLayout.put(thisPanelKey, thisPanelIndex + 1);
        updateLayout();
    }

    private void movePanelLeft(ControlPanel panel) {
        logger.debug("Move Panel Left " + panel.getModule().type);

        String thisPanelKey = panel.getModule().getConnectionKey();
        int thisPanelIndex = panelLayout.get(thisPanelKey);

        for(ControlPanel p : panels) {
            String key = p.getModule().getConnectionKey();
            int displayIndex = panelLayout.get(key);
            if(displayIndex == thisPanelIndex - 1) {
                panelLayout.put(key, thisPanelIndex);
                break;
            }
        }
        panelLayout.put(thisPanelKey, thisPanelIndex - 1);
        updateLayout();
    }

    private void updateLayout() {
        logger.debug("Update Layout");
        int numPanels = panels.size();
        int width = app.getWidth();
        int totalWidth = (numPanels * panelWidth) + ((numPanels - 1) * spacing);
        int leftBorder = (width - totalWidth) / 2;

        for(ControlPanel p : panels) {
            int displayIndex = panelLayout.get(p.getModule().getConnectionKey());
            p.setPosition(leftBorder + ((panelWidth + spacing) * displayIndex), top);
            p.canMoveRight = displayIndex < numPanels - 1;
            p.canMoveLeft = displayIndex > 0;
        }

        JSONObject config = app.getConfig();

        JSONArray layoutConfig = new JSONArray();
        for(Map.Entry<String, Integer> entry : panelLayout.entrySet()) {
            layoutConfig.setString(entry.getValue(), entry.getKey());
        }

        config.setJSONArray(ConfigKeys.MODULE_LAYOUT, layoutConfig);
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
        for(ControlPanel panel : panels) {
            panel.hide();
        }
    }

    public void show() {
        for(ControlPanel panel : panels) {
            panel.show();
        }
    }
}
