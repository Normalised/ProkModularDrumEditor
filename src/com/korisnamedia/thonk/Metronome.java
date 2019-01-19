package com.korisnamedia.thonk;

import com.prokmodular.ProkModule;
import com.prokmodular.comms.CommandContents;
import com.prokmodular.comms.Commands;
import com.prokmodular.comms.ModuleSerialConnection;
import org.slf4j.Logger;

import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Math.round;
import static org.slf4j.LoggerFactory.getLogger;

class MetronomeTask extends TimerTask {

    private final Metronome metronome;

    public MetronomeTask(Metronome m) {
        metronome = m;
    }

    @Override
    public void run() {
        metronome.tick();
    }
}

public class Metronome {

    final Logger logger = getLogger(Metronome.class);

    private ProkModule module;
    private final MetronomeTask timerTask;
    private int interval = 500;
    private int count;
    private Timer metronomeTimer;
    private boolean active;

    public Metronome() {
        timerTask = new MetronomeTask(this);
        active = false;
    }

    public void start() {
        metronomeTimer = new Timer();
        metronomeTimer.schedule(timerTask, 0, 1);
    }

    public void setBPM(float bpm) {
        interval = round(60000 / bpm);
    }

    public void tick() {

        if(!active) return;
        count++;

        if(count >= interval) {
            count = 0;
            if(module != null) {
                module.trigger();
            }
        }
    }


    public void on(boolean active) {
        logger.debug("Metronome active " + active);
        this.active = active;
        if(active && metronomeTimer == null) {
            start();
        }
    }

    public void stop() {
        if(metronomeTimer != null) {
            metronomeTimer.cancel();
        }
    }

    public void setModule(ProkModule prokModule) {
        module = prokModule;
    }
}
