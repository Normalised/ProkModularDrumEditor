package com.korisnamedia.thonk.ui;

import com.prokmodular.ui.ModelUIBuilder;
import com.prokmodular.model.ModelConfig;
import com.prokmodular.model.ModelUI;
import com.prokmodular.model.ProkModel;
import controlP5.Button;
import controlP5.CallbackEvent;
import controlP5.ControlP5;
import controlP5.Slider;

import java.util.ArrayList;
import java.util.List;

import static com.prokmodular.model.ParameterMapping.createLinear;
import static com.prokmodular.model.ParameterMapping.createSquared;
import static com.korisnamedia.thonk.ui.Layout.leftMargin;

public class HiHatUIForProcessing implements ModelUI, UIForProcessing {

    private final ArrayList<Slider> freqSliders;
    private final ArrayList<Slider> gainSliders;

    private List<Float> frequencyBuffer;
    private Button copyFrqButton;
    private Button pasteFrqButton;

    public HiHatUIForProcessing() {
        freqSliders = new ArrayList<>();
        gainSliders = new ArrayList<>();

        frequencyBuffer = new ArrayList<>(6);

        frequencyBuffer.add(0,205.3f);
        frequencyBuffer.add(1,304.4f);
        frequencyBuffer.add(2,369.6f);
        frequencyBuffer.add(3,522.7f);
        frequencyBuffer.add(4,540.0f);
        frequencyBuffer.add(5,800.0f);
    }

    public void copyFrequencies() {
        for(int i=0;i<6;i++) {
            frequencyBuffer.set(i,freqSliders.get(i).getValue());
        }
    }

    public void pasteFrequencies() {
        for(int i=0;i<6;i++) {
            freqSliders.get(i).setValue(frequencyBuffer.get(i));
        }
    }

    public void resetTo808() {

        freqSliders.get(0).setValue(205.3f);
        freqSliders.get(1).setValue(304.4f);
        freqSliders.get(2).setValue(369.6f);
        freqSliders.get(3).setValue(522.7f);
        freqSliders.get(4).setValue(540.0f);
        freqSliders.get(5).setValue(800.0f);

        for (Slider s : gainSliders) {
            s.setValue(100.0f);
        }

    }

    public void resetToDR110() {

        freqSliders.get(0).setValue(317f);
        freqSliders.get(1).setValue(465f);
        freqSliders.get(2).setValue(820f);
        freqSliders.get(3).setValue(1150f);

        // 5 and 6 are silent.
        freqSliders.get(4).setValue(540.0f);
        freqSliders.get(5).setValue(800.0f);

        for (Slider s : gainSliders) {
            s.setValue(100.0f);
        }

        gainSliders.get(4).setValue(0f);
        gainSliders.get(5).setValue(0f);
    }

    @Override
    public void createUI(ModelUIBuilder ui, int firmwareVersion, int version) {

        //ui.addKnob("Base Freq", 0, 1000);
        ui.addSlider("Base Freq", 0, 1000);

        freqSliders.add((Slider) ui.addTunableSlider("Square Freq 1", 30, 5000));
        freqSliders.add((Slider) ui.addTunableSlider("Square Freq 2", 30, 5000));
        freqSliders.add((Slider) ui.addTunableSlider("Square Freq 3", 30, 5000));
        freqSliders.add((Slider) ui.addTunableSlider("Square Freq 4", 30, 5000));
        freqSliders.add((Slider) ui.addTunableSlider("Square Freq 5", 30, 5000));
        freqSliders.add((Slider) ui.addTunableSlider("Square Freq 6", 30, 5000));

        ui.addSpace();

        gainSliders.add((Slider) ui.addSlider("Square Gain 1", createLinear(0, 100, 0, 1)));
        gainSliders.add((Slider) ui.addSlider("Square Gain 2", createLinear(0, 100, 0, 1)));
        gainSliders.add((Slider) ui.addSlider("Square Gain 3", createLinear(0, 100, 0, 1)));
        gainSliders.add((Slider) ui.addSlider("Square Gain 4", createLinear(0, 100, 0, 1)));
        gainSliders.add((Slider) ui.addSlider("Square Gain 5", createLinear(0, 100, 0, 1)));
        gainSliders.add((Slider) ui.addSlider("Square Gain 6", createLinear(0, 100, 0, 1)));

        ui.addSpace();

        ui.addMixerChannel("Source Noise");
        ui.addNoiseSampleRate();

        ui.nextColumn();

        // 2 bandpass filters
        ui.addBiquad("Band Pass Low", 80, 10000);
        ui.addBiquad("Band Pass Mid/High", 500, 10000);

        ui.addSpace();

        // Band Pass Input Gains
        ui.addSlider("BP Gain Low", createSquared(0, 100, 0, 1));
        ui.addSlider("BP Gain Mid/High", createSquared(0, 100, 0, 1));

        // 2 shapers
        ui.addSlider("Shaper Mid/High", 1, 100);

        ui.addSpace();

        ui.addSlider("Osc Attack", createSquared(0, 100, 0, 32000));

        ui.addSpace();

        // 3 envelopes
        ui.addSlider("Decay Low", createSquared(0, 100, 1, 16000));
        ui.addSlider("Low Extend", createLinear(0, 100, 0, 32000));
        if(version > 5) {
            ui.addIntSlider("Low Extend Factor", -8,64);
        }
        ui.addSlider("Decay Mid", createSquared(0, 100, 1, 16000));
        ui.addSlider("Mid Extend", createLinear(0, 100, 0, 32000));
        if(version > 5) {
            ui.addIntSlider("Mid Extend Factor", -8,64);
        }

        ui.addSlider("Decay High", createSquared(0, 100, 1, 16000));
        ui.addSlider("High Extend", createLinear(0, 100, 0, 32000));
        if(version > 5) {
            ui.addIntSlider("High Extend Factor", -8,64);
        }

        ui.addSpace();

        ui.addSlider("Noise Attack", createSquared(0, 100, 0, 32000));
        ui.addSlider("Decay Noise", createSquared(0, 100, 1, 8000));
        ui.addSlider("Noise Extend", createLinear(0, 100, 0, 32000));
        if(version > 5) {
            ui.addIntSlider("Noise Extend Factor", -8,64);
        }

        ui.nextColumn();

        ui.addBiquad("LPF Low", 60, 3000);
        ui.addBiquad("HPF Mid", 80, 6000);
        ui.addBiquad("HPF High", 80, 10000);

        ui.addSpace();

        ui.addMixerChannel("Low Out");
        ui.addMixerChannel("Mid Out");
        ui.addMixerChannel("High Out");

        ui.addSpace();

        ui.addBiquad("BPF Noise", 80, 12000);

        ui.addMixerChannel("Noise");

        ui.addSpace();
        ui.addMixerChannel("Main Output");

    }

    public void createExtraUI(ControlP5 cp5) {
        copyFrqButton = cp5.addButton("CopyFrqs")
                .setValue(0)
                .setSize(50, 20)
                .setLabel("Copy Frqs")
                .setPosition(leftMargin, 600)
                .setColorBackground(0xFF007700)
                .onRelease((CallbackEvent theEvent) -> {
                    copyFrequencies();
                });

        pasteFrqButton = cp5.addButton("PasteFrqs")
                .setValue(0)
                .setSize(50, 20)
                .setLabel("Paste Frqs")
                .setPosition(leftMargin + 60, 600)
                .setColorBackground(0xFF007777)
                .onRelease((CallbackEvent theEvent) -> {
                    pasteFrequencies();
                });
    }

    public void removeExtraUI(ControlP5 cp5) {
        cp5.remove("CopyFrqs");
        cp5.remove("PasteFrqs");
    }
}
