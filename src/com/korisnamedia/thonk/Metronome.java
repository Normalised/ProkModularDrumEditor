package com.korisnamedia.thonk;

import com.prokmodular.comms.Commands;
import com.prokmodular.comms.SerialCommunicator;

import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Math.round;

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

    private final SerialCommunicator comms;
    private final MetronomeTask timerTask;
    private int interval;
    private int count;
    private Timer metronomeTimer;
    private boolean active;

    public Metronome(SerialCommunicator sc) {
        comms = sc;
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
            comms.sendCommand(Commands.TRIGGER, "");
        }
    }


    public void on(boolean active) {
        this.active = active;
    }

    public void stop() {
        if(metronomeTimer != null) {
            metronomeTimer.cancel();
        }
    }
}
