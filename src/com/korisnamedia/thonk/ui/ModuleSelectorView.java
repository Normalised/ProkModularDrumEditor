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
    private List<ProkModule> modules;
    private List<ControlPanel> panels;

    private HashMap<String, Integer> panelLayout;
    private ProkModule bankSelectModule;

    private PresetManager presetManager;

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

        panelLayout = new HashMap<>();

        if(app.getConfig().hasKey(ConfigKeys.MODULE_LAYOUT)) {
            JSONArray layoutConfig = app.getConfig().getJSONArray(ConfigKeys.MODULE_LAYOUT);

            for(int i=0;i<layoutConfig.size();i++) {
                String key = layoutConfig.getString(i);
                panelLayout.put(key, i);
            }
        }
    }

    public void showAvailableModules(List<ProkModule> modulesToUse) {
        logger.debug("Show Available Modules " + modulesToUse.size());

        panels = new ArrayList<>();
        int index = 0;
        int numModules = modulesToUse.size();
        int displayIndex = 0;
        for(ProkModule module : modulesToUse) {
            ControlPanel panel = new ControlPanel(cp5, module.type);
            String moduleKey = module.getConnectionKey();
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

            panel.setPanelImage(getImageForModule(module.type));
            panel.setVerticalLayout(true);
            panel.setModule(module);
            panel.setSize(panelWidth, panelHeight + 150);
            panel.onRelease(theEvent -> {
                Pointer p = panel.getPointer();
                logger.debug(p.toString());
                int x = p.x();
                int y = p.y();

                // Panel sort arrows
                if(y <= 20) {
                    if(x < panel.getWidth() / 2 && panel.canMoveLeft) {
                        movePanelLeft(panel);
                    } else if(x > panel.getWidth() / 2 && panel.canMoveRight) {
                        movePanelRight(panel);
                    }
                    return;
                }

                // Quad LEDS
                if(x >= 12 && x <= 48 && y >= 48 && y <= 84 ) {
                    int qx = (x - 12) / 18;
                    int qy = (y - 48) / 22;
                    logger.debug("Q " + qx + ", " + qy);
                    int q = 0;

                    if(qx == 0) {
                        q = qy == 0 ? 3 : 0;
                    } else {
                        q = qy == 0 ? 2 : 1;
                    }
                    module.selectQuad(q);
                    module.morphX(qx * 1000);
                    module.morphY((1 - qy) * 1000);
                    return;
                }

                // Trigger button / SD
                if(y >= 235 && y <= 265) {
                    if(x >= 25 && x <= 55) {
                        logger.debug("Sending trigger to " + module.type);
                        module.trigger();
                    } else if(x < 22) {
                        loadBankIntoModule(module);
                    }

                } else {
                    app.moduleSelected(module);
                }
            });
            panels.add(panel);
            index++;
        }
        updateLayout();
        this.modules = modulesToUse;
    }

    private void loadBankIntoModule(ProkModule module) {
        logger.debug("Load bank into module " + module.getConnectionKey());
        bankSelectModule = module;
        app.selectFolder("Choose Bank Folder", "bankFolderSelected", null, this);
    }

    public void bankFolderSelected(File selectedFolder) {
        logger.debug("Selected folder " + selectedFolder.getAbsolutePath() + " for " + bankSelectModule.getConnectionKey());

        presetManager.setCurrentModel(bankSelectModule.model);
        List<File> files = presetManager.listFilesFrom(selectedFolder);
        int numFiles = files.size();
        if(numFiles > 16) numFiles = 16;
        int paramIndex = 0;
        for(int i=0;i<numFiles;i++) {
            try {
                Preset p  = presetManager.readPreset(files.get(i));
                paramIndex = 0;
                for(Float f : p.params) {
                    bankSelectModule.setParam(new ParamMessage(paramIndex++, f));
                }
                bankSelectModule.saveModel(i);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
