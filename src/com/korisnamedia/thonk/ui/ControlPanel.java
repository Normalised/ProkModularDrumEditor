package com.korisnamedia.thonk.ui;

import com.prokmodular.ProkModule;
import com.prokmodular.comms.Messages;
import controlP5.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import processing.core.PGraphics;
import processing.core.PImage;

import java.util.HashMap;

import static controlP5.ControlP5.BitFontStandard56;
import static java.lang.Integer.parseInt;

public class ControlPanel extends Controller<ControlPanel> {
    final Logger logger = LoggerFactory.getLogger(ControlPanel.class);

    public static final int MORPH_ID = 100;

    private final ControlP5 cp5;
    private final ControlPanelView controlPanelView;

    private HashMap<String, String> nameMap;

    private ProkModule module;
    private boolean verticalLayout = false;
    public boolean canMoveLeft = false;
    public boolean canMoveRight = false;

    public static int ACTION_MODULE_SD = 5000;
    public static int ACTION_LAYOUT_MOVE_LEFT = 5001;
    public static int ACTION_LAYOUT_MOVE_RIGHT = 5002;
    public static int ACTION_PANEL_CLICKED = 5003;
    public static int ACTION_MODULE_TRIGGER = 5004;

    PImage panelImage;

    public ControlPanel(ControlP5 cp5, String uid) {
        super(cp5, uid);
        this.cp5 = cp5;

        nameMap = new HashMap<>();
        nameMap.put("kick", "BD");
        nameMap.put("snare", "SD");
        nameMap.put("hihat", "HH");
        nameMap.put("clap", "CP");

        logger.debug("Create ControlPanel");

        controlPanelView = new ControlPanel.ControlPanelView();
        setView(controlPanelView);
    }

    public void setModule(ProkModule moduleToUse) {
        module = moduleToUse;
    }

    public ProkModule getModule() {
        return module;
    }

    public void setVerticalLayout(boolean vert) {
        verticalLayout = vert;
    }

    public void setPanelImage(PImage imageToUse) {
        panelImage = imageToUse;
    }

    void onSDClicked(CallbackListener listener) {
        addListenerFor(ACTION_MODULE_SD, listener);
    }

    void onMoveLeftClicked(CallbackListener listener) {
        addListenerFor(ACTION_LAYOUT_MOVE_LEFT, listener);
    }

    void onMoveRightClicked(CallbackListener listener) {
        addListenerFor(ACTION_LAYOUT_MOVE_RIGHT, listener);
    }

    void onPanelClicked(CallbackListener listener) {
        addListenerFor(ACTION_PANEL_CLICKED, listener);
    }

    void onTriggerClicked(CallbackListener listener) {
        addListenerFor(ACTION_MODULE_TRIGGER, listener);
    }

    @Override
    protected void onRelease() {
        Pointer p = getPointer();
        logger.debug(p.toString());
        int x = p.x();
        int y = p.y();

        // Panel sort arrows
        if(y <= 20) {
            if(x < getWidth() / 2 && canMoveLeft) {
                callListener(ACTION_LAYOUT_MOVE_LEFT);
            } else if(x > getWidth() / 2 && canMoveRight) {
                callListener(ACTION_LAYOUT_MOVE_RIGHT);
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
                callListener(ACTION_MODULE_TRIGGER);
            } else if(x < 22) {
                callListener(ACTION_MODULE_SD);
            }
        } else {
            callListener(ACTION_PANEL_CLICKED);
        }
    }

    private class ControlPanelView implements controlP5.ControllerView<ControlPanel> {

        @Override
        public void display(PGraphics graphics, ControlPanel controlPanel) {

            int x = 0;
            int y = verticalLayout ? 20 : 0;
            if(controlPanel.panelImage != null) {
                graphics.image(controlPanel.panelImage, 0, y, getWidth(), getHeight() - 150);
                y = getHeight() - 120;
            }

            int textY = 9;
            graphics.textFont(BitFontStandard56);

            if(verticalLayout) {
                drawReorderArrows(graphics);
                y += 6;

            } else {
                y = textY;
            }

            showNameAndVersion(graphics, nameMap.get(controlPanel.module.type),
                    controlPanel.module.getVersion(),
                    controlPanel.module.getFirmwareVersion(), x, y);

            if(verticalLayout) {

                x = getWidth() - 10;
                y = getHeight() - 120;

            } else {
                x = getWidth() - 20;
                y = getHeight() - 20;
            }

            showConnected(graphics, controlPanel.module.isConnected(), x, y);

            if(verticalLayout) {
                x = 0;
                y += 18;
            } else {
                x = 60;
                y = textY;
            }

            showSD(graphics, controlPanel.module.hasSD, x, y);

            if(verticalLayout) {
                y += 12;
            } else {
                x = 108;
                y = textY;
            }

            showMemory(graphics, "Mem " + controlPanel.module.getProperty(Messages.AUDIO_MEMORY), x, y);

            if(verticalLayout) {
                y += 12;
            } else {
                x = 160;
                y = textY;
            }

            showBlocks(graphics, controlPanel.module.getProperty(Messages.BLOCK_SIZE), x, y);

            if(verticalLayout) {
                y += 12;
            } else {
                x = 226;
                y = textY;
            }

            showCPU(graphics, controlPanel.module.getProperty(Messages.CPU), x, y);

        }

        private void drawReorderArrows(PGraphics graphics) {
            graphics.fill(ControlP5Constants.WHITE);
            graphics.stroke(ControlP5Constants.WHITE);
            graphics.strokeWeight(4);
            int arrowWidth = 8;
            int arrowHeight = 6;
            int top = 2;
            if(canMoveLeft) {
                int lx = 12;
                graphics.line(lx + arrowWidth,top, lx, top + arrowHeight);
                graphics.line(lx, top + arrowHeight,lx + arrowWidth,top + (arrowHeight * 2));
            }
            if(canMoveRight) {
                int rx = getWidth() - 20;
                graphics.line(rx ,top, rx + arrowWidth, top + arrowHeight);
                graphics.line(rx + arrowWidth, top + arrowHeight, rx,top + (arrowHeight * 2));
            }
        }

        private void showBlocks(PGraphics graphics, String blocks, int x, int y) {
            graphics.fill(ControlP5Constants.WHITE);
            graphics.text("Blocks " + blocks, x, y);
        }

        private void showSD(PGraphics graphics, boolean hasSD, int x, int y) {
            graphics.fill(ControlP5Constants.WHITE);
            if (hasSD) {
                graphics.text("Has SD", x, y);
            } else {
                graphics.text("No SD", x, y);
            }

        }

        private void showNameAndVersion(PGraphics graphics, String name, int version, int firmwareVersion, int x, int y) {
            if (name.length() == 0) return;
            graphics.fill(ControlP5Constants.WHITE);
            graphics.text(name + " v" + firmwareVersion + "." + version, x, y);
        }

        private void showConnected(PGraphics graphics, boolean connected, int x, int y) {

            graphics.noStroke();
            if (connected) {
                graphics.fill(0, 255, 0);
            } else {
                graphics.fill(255, 0, 0);
            }

            graphics.rect(x, y, 10, 10);
        }

        private void showMemory(PGraphics graphics, String audiomem, int x, int y) {
            graphics.textFont(BitFontStandard56);
            graphics.fill(ControlP5Constants.WHITE);
            graphics.text(audiomem, x, y);
        }


        void showCPU(PGraphics graphics, String cpu, int x, int y) {
            graphics.fill(ControlP5Constants.WHITE);
            graphics.text("CPU " + cpu + "%", x, y);
        }

    }
}
