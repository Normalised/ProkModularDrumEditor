package com.korisnamedia.thonk.ui;

import com.korisnamedia.thonk.ThonkModularApp;
import com.prokmodular.comms.Serial;
import controlP5.ControlP5;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;

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

    private int numModules = 0;
    private boolean visible = false;
    private Map<String, List<Serial>> modulePorts;

    private int panelWidth = 60;
    private int panelHeight = -1;
    private int spacing = 10;
    private int top = 50;

    public ModuleSelectorView(PGraphics graphics, ControlP5 cp5, ThonkModularApp thonkModularApp) {
        this.cp5 = cp5;
        this.graphics = graphics;
        app = thonkModularApp;
        bdPanel = app.loadImage("img/Prok_Drums_BD.jpg");
        snPanel = app.loadImage("img/Prok_Drums_SN.jpg");
        cpPanel = app.loadImage("img/Prok_Drums_CP.jpg");
        hhPanel = app.loadImage("img/Prok_Drums_HH.jpg");

        panelHeight = (panelWidth / bdPanel.width) * bdPanel.height;
    }

    public void showAvailableModule(Map<String, List<Serial>> modulePorts) {
        logger.debug("Show Available Modules " + modulePorts.size());

        for(Map.Entry<String, List<Serial>> portsPerDrum : modulePorts.entrySet()) {
            numModules += portsPerDrum.getValue().size();
        }
        this.modulePorts = modulePorts;
        visible = true;
    }

    public void draw() {
        if(!visible) return;
        int width = app.getWidth();
        int totalWidth = (numModules * panelWidth) + ((numModules - 1) * spacing);
        int leftBorder = (width - totalWidth) / 2;
        for(Map.Entry<String, List<Serial>> portsPerDrum : modulePorts.entrySet()) {
            graphics.image(getImageForModule(portsPerDrum.getKey()),leftBorder,top, panelWidth, panelHeight);
        }
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
}
