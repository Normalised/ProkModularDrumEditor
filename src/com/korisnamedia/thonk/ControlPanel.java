package com.korisnamedia.thonk;

import com.prokmodular.comms.Commands;
import com.prokmodular.comms.Messages;
import controlP5.ControlP5;
import controlP5.ControlP5Constants;
import processing.core.PGraphics;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Integer.parseInt;
import static processing.core.PApplet.println;

public class ControlPanel {
    public static final int METRONOME_ID = 101;
    public static final int MORPH_ID = 100;

    private final ControlP5 cp5;
    private final ThonkModularApp app;
    private final PGraphics graphics;
    private MorphAndStorage morphAndStorage;

    private HashMap<String, String> nameMap;
    private boolean devMode = true;

    public ControlPanel(PGraphics graphics, ControlP5 cp5, ThonkModularApp thonkModularApp) {
        this.cp5 = cp5;
        this.graphics = graphics;
        app = thonkModularApp;

        nameMap = new HashMap<>();
        nameMap.put("kick","BD");
        nameMap.put("snare","SD");
        nameMap.put("hihat","HH");
        nameMap.put("clap","CP");

        println("Create ControlPanel");
        morphAndStorage = new MorphAndStorage(graphics, cp5, app);
    }

    public void createDefaultControls(Map<String, String> moduleState) {

        addTools();

        addMetronome();
        morphAndStorage.create(0,40);

        morphAndStorage.setState(moduleState);

        cp5.addToggle("Exclusive")
                .setPosition(300, app.height - 24)
                .setSize(20,20)
                .onChange(theEvent -> {
                    boolean on = theEvent.getController().getValue() > 0;
                    app.setExclusive(on);
                }).setValue(false);
    }

    private void addTools() {
        if(devMode) {
            cp5.addButton("Generate Header").setPosition(10,10).setSize(95,20).onRelease(theEvent -> {
                app.generateHeader();
            });

            cp5.addButton("Save Bank").setPosition(115,10).setSize(95,20).onRelease(theEvent -> {
                app.saveModelsLocally();
            });
        } else {
            cp5.addButton("Save Bank").setPosition(65,10).setSize(95,20).onRelease(theEvent -> {
                app.saveModelsLocally();
            });

        }
    }

    private void addMetronome() {
        int y = app.height - 24;

        cp5.addToggle("Metro")
                .setPosition(10, y)
                .setSize(20,20)
                .setLabelVisible(false)
                .onChange(theEvent -> {
            boolean on = theEvent.getController().getValue() > 0;
            app.useMetronome(on);
        }).setValue(false);

        app.ui.addLocalSlider("Tempo", 10, 400, METRONOME_ID + 1).setPosition(32, y).setSize(178,20);
    }

    public void showStatus(boolean connected, Map<String, String> moduleState) {

        showConnected(connected);

        int x = app.width - 400;
        int y = app.height - 24;

        showNameAndVersion(nameMap.get(moduleState.get("name")),
                moduleState.get(Messages.VERSION),
                moduleState.get(Messages.FIRMWARE_VERSION), x - 40, y);
        showSD(moduleState.get(Messages.HAS_SD_CARD).equalsIgnoreCase("1"), x + 30, y);
        showMemory("MEM " + moduleState.get(Messages.AUDIO_MEMORY), x + 80, y);
        showBlocks(moduleState.get(Messages.BLOCK_SIZE), x + 140, y + 15);
        showCPU(moduleState.get(Messages.CPU),x + 210, y);
    }

    private void showBlocks(String blocks, int x, int y) {
        graphics.fill(ControlP5Constants.WHITE);
        graphics.text("Blocks " + blocks, x, y);
    }

    private void showSD(boolean hasSD, int x, int y) {
        graphics.fill(ControlP5Constants.WHITE);
        if(hasSD) {
            graphics.text("Has SD", x, y + 15);
        } else {
            graphics.text("No SD", x, y + 15);
        }

    }

    private void showNameAndVersion(String name, String version, String firmwareVersion, int x, int y) {
        if(name.length() == 0) return;
        graphics.fill(ControlP5Constants.WHITE);
        graphics.text(name + " v" + version + "." + firmwareVersion, x, y + 15);
    }

    private void showConnected(boolean connected) {

        graphics.noStroke();
        if(connected) {
            graphics.fill(0,255,0);
        } else {
            graphics.fill(255,0,0);
        }

        graphics.rect(app.width - 20,app.height - 20,10,10);
    }

    private void showMemory(String audiomem, int x, int y) {
        graphics.fill(ControlP5Constants.WHITE);
        graphics.text(audiomem, x + 8, y + 15);
    }


    void showCPU(String cpu, int x, int y) {
        graphics.noStroke();
        graphics.fill(0,45,90);
        graphics.rect(x + 60,y,100,20);
        graphics.fill(0,116,217);
        graphics.rect(x + 60,y,parseInt(cpu),20);
        graphics.fill(ControlP5Constants.WHITE);
        graphics.text("CPU " + cpu + "%",x, y + 15);
    }
}
