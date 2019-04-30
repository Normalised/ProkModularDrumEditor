package com.korisnamedia.thonk.ui;

import com.prokmodular.ProkModule;
import com.prokmodular.model.ParameterMapping;
import com.korisnamedia.thonk.tuning.NoteMapper;
import com.prokmodular.ui.ModelUIBuilder;
import controlP5.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;

import static com.korisnamedia.thonk.ui.ControlPanel.MORPH_ID;
import static com.korisnamedia.thonk.ui.ModuleEditorView.METRONOME_ID;
import static com.prokmodular.model.ParameterMapping.createLinear;
import static com.prokmodular.model.ParameterMapping.createNone;
import static com.prokmodular.model.ParameterMapping.createSquared;

public class UIControls implements ControlListener, ModelUIBuilder {

    final Logger logger = LoggerFactory.getLogger(UIControls.class);

    private final ModuleEditorView app;
    public ControlP5 cp5;

    private ArrayList<Controller> controls;
    private HashMap<Integer, Slider> tunableControls;

    protected int currentColumn = 0;
    protected int currentRow = 0;
    private Layout layout;

    private int lastParamID = -1;

    private Textfield noteNumberInput;

    private NoteMapper noteMapper;
    private Button setNoteButton;
    private Button doubleFreqButton;
    private Button halveFreqButton;
    private ProkModule module;
    private int firmwareVersion = 0;

    private int extendFactorMinimum = 1;
    private int decayMax = 32000;

    public UIControls(ModuleEditorView view, ControlP5 cp5) {
        layout = new Layout();
        controls = new ArrayList<>();
        tunableControls = new HashMap<>();

        noteMapper = new NoteMapper();

        app = view;
        this.cp5 = cp5;
        this.cp5.addListener(this);
    }

    public void setModule(ProkModule moduleToUse) {
        clear();

        module = moduleToUse;
        firmwareVersion = module.getFirmwareVersion();
        if(firmwareVersion > 3) {
            extendFactorMinimum = -8;
            decayMax = 98000;
        }
        module.ui.createUI(this, module.getFirmwareVersion(), module.getVersion());
        if(module.ui instanceof UIForProcessing) {
            ((UIForProcessing) module.ui).createExtraUI(cp5);
        }
    }

    public void createNoteControls() {
        setNoteButton =  cp5.addButton("Set Note")
                .setPosition(layout.leftMargin, app.getHeight() - 98)
                .setSize(100, 16)
                .onRelease(theEvent -> setNote());

        doubleFreqButton = cp5.addButton("Double")
                .setPosition(layout.leftMargin, app.getHeight() - 78)
                .setSize(100, 16)
                .onRelease(theEvent -> retuneByFactor(2.0f));

        halveFreqButton = cp5.addButton("Halve")
                .setPosition(layout.leftMargin, app.getHeight() - 58)
                .setSize(100, 16)
                .onRelease(theEvent -> retuneByFactor(0.5f));

        noteNumberInput = cp5.addTextfield("Note Number", layout.leftMargin + 110, app.getHeight() - 100, 80,20)
                .onChange(theEvent -> setNote())
                .setCaptionLabel("");

        noteNumberInput.setAutoClear(false);
        //noteNumberInput.setInputFilter(ControlP5Constants.INTEGER);

        //noteNumberInput.onEnter(theEvent -> setNote());
    }

    private void retuneByFactor(float factor) {
        for(Slider s : tunableControls.values()) {
            s.setValue(s.getValue() * factor);
        }
    }

    private void setNote() {
        logger.debug("Set Note " + lastParamID + " : " + tunableControls.containsKey(lastParamID));

        if(noteNumberInput.getText().contains(",")) {
            // assume list
            String[] notes = noteNumberInput.getText().split(",");
            logger.debug("Setting multiple notes : " + notes.length);
            int controlIndex = 0;
            Object[] controls = tunableControls.values().toArray();
            for(String noteText : notes) {
                noteText = noteText.trim();
                if(noteText.equalsIgnoreCase("-")) {
                    controlIndex++;
                    continue;
                } else {
                    Integer noteNumber = Integer.parseInt(noteText);
                    float frequency = (float) noteMapper.getFrequency(noteNumber);
                    if(controlIndex < controls.length) {
                        ((Slider) controls[controlIndex]).setValue(frequency);
                    }
                    controlIndex++;
                }
            }
        } else {

            if (lastParamID != -1 && tunableControls.containsKey(lastParamID)) {
                try {
                    Integer noteNumber = Integer.parseInt(noteNumberInput.getText());
                    Slider s = tunableControls.get(lastParamID);
                    float frequency = (float) noteMapper.getFrequency(noteNumber);
                    logger.debug("Setting slider " + lastParamID + " to note number " + noteNumber + " with freq " + frequency);
                    s.setValue(frequency);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Object addSlider(String name, int low, int high) {
        return addSlider(name, createNone(low, high));
    }

    public Object addIntSlider(String name, int low, int high) {
        return addIntSlider(name, createNone(low, high));
    }

    public Object addIntSlider(String name, ParameterMapping mapping) {
        return addSlider(name, mapping).setNumberOfTickMarks(mapping.getInputRange() + 1).showTickMarks(false);
    }

    public Slider addLocalSlider(String name, int low, int high, int id) {
        Slider s = cp5.addSlider(name);
        s.setId(id);
        s.setRange(low, high);
        return s;
    }

    public Knob addKnob(String name, int low, int high) {
        return addKnob(name, createNone(low, high));
    }

    public Knob addKnob(String name, ParameterMapping mapping) {

        int id = app.getCurrentParamId();

        Knob k = cp5.addKnob(name);

        k.setId(id + 1)
            .setPosition((currentColumn * layout.columnWidth) + layout.leftMargin, (currentRow * layout.rowHeight) + layout.topMargin)
            .setSize(50, 20)
                .setCaptionLabel(name)
            .setRange(mapping.inLow, mapping.inHigh)
            .setValue(mapping.inLow + (mapping.getInputRange() / 2));

        controls.add(id, k);
        app.addParameter(mapping);

        currentRow += 3;

        return k;
    }

    public Slider addSlider(String name, ParameterMapping mapping) {

        int id = app.getCurrentParamId();

        Slider s = cp5.addSlider(name);

        s.setId(id + 1)
                .setPosition((currentColumn * layout.columnWidth) + layout.leftMargin, (currentRow * layout.rowHeight) + layout.topMargin)
                .setSize(200, 20)
                .setRange(mapping.inLow, mapping.inHigh)
                .setValue(mapping.inLow + (mapping.getInputRange() / 2));

        controls.add(id, s);
        app.addParameter(mapping);

        currentRow++;

        return s;
    }

    public Slider addTunableSlider(String name, ParameterMapping mapping) {

        Slider slider = addSlider(name, mapping);

        tunableControls.put(slider.getId() - 1, slider);

        return slider;
    }

    public Object addTunableSlider(String name, int low, int high) {
        Slider slider = (Slider) addSlider(name, low, high);
        tunableControls.put(slider.getId() - 1, slider);

        return slider;
    }

    @Override
    public void controlEvent(ControlEvent theEvent) {
        int paramID = theEvent.getController().getId() - 1;

//        logger.debug("Control event " + paramID);

        if(paramID > -1) {
            if(lastParamID != paramID && tunableControls.containsKey(lastParamID)) {
                tunableControls.get(lastParamID).setColorBackground(0xFF003652);
            }
            lastParamID = paramID;

            if(tunableControls.containsKey(lastParamID)) {
                tunableControls.get(lastParamID).setColorBackground(0xFF1a4212);
            }
        }

        float val = theEvent.getController().getValue();
        if (paramID < 0) {
            return;
        } else if (paramID >= 100) {
            handleNonParamEvent(theEvent);
            return;
        }

        app.handleControlEvent(paramID, val);
    }

    private void handleNonParamEvent(ControlEvent event) {
        int paramID = event.getController().getId() - 1;

        if (paramID == METRONOME_ID) {
            app.setBPM(event.getController().getValue());
            return;
        }

        if (paramID == MORPH_ID) {
            float newX = event.getController().getArrayValue(0);
            float newY = event.getController().getArrayValue(1);
            app.setMorph(newX,newY);

        }
    }

    public void setCurrentParam(int paramID, float val) {
        Controller c = controls.get(paramID);
        logger.debug("setCurrentParam " + paramID + " -> " + val);
        c.setValueSelf(val);
    }

    public void setControlValue(int index, float value) {
        Controller c = controls.get(index);
        c.setValue(value);
    }

    public void addSpace() {
        currentRow++;
    }

    public void nextColumn() {
        currentColumn++;
        currentRow = 0;
    }

    public void addNoiseSampleRate() {
        addSlider("NoiseSampleRate", createLinear(1, 100, 0.01f, 1));
    }

    public void addSineWithEnvelope(String name) {
        addSineWithEnvelope(name, decayMax);
    }

    public void addTriModWithEnvelope(String name) {
        addTriModWithEnvelope(name, decayMax);
    }

    public void addSineWithEnvelope(String name, int decay) {
        addTunableSlider(name + " Base Freq", createNone(30, 5000));
        addOsc(name, decay);
    }

    public void addTriModWithEnvelope(String name, int decay) {
        addTunableSlider(name + " Base Freq", createNone(1, 5000));
        addSlider(name + " Pulse Width", createLinear(0, 100, 0, 1));
        addOsc(name, decay);
    }

    private void addOsc(String name, int decay) {
        addSlider(name + " Freq Attack", createSquared(0, 100, 0, 32000));
        addSlider(name + " Freq Decay", createSquared(1, 100, 1, decay));
        addSlider(name + " Freq Amount", createSquared(0, 100, 0, 10000));
        addSlider(name + " Freq Extend Level", extendLevelMapping());
        addIntSlider(name + " Freq Extend Factor", createNone(extendFactorMinimum, 64));
    }

    private ParameterMapping extendLevelMapping() {
        return createLinear(0, 100, 0, 32767);
    }

    public void addShortExpEnv(String name) {
        addSlider(name + " Attack", createSquared(0, 100, 0, 8000));
        addSlider(name + " Decay", createSquared(0, 100, 1, 12000));
        addSlider(name + " Extend Level", extendLevelMapping());
        addIntSlider(name + " Extend Factor", createNone(extendFactorMinimum, 64));
    }

    public void addADEnvelope(String name) {
        addSlider(name + " Attack", createSquared(0, 100, 0, 32000));
        addSlider(name + " Decay", createSquared(0, 100, 1, decayMax));
        addSlider(name + " Extend Level", extendLevelMapping());
        addIntSlider(name + " Extend Factor", createNone(extendFactorMinimum, 64));
    }

    public void addBiquad(String name, int low, int high) {
        addSlider(name + " Cutoff", createNone(low, high));
        addSlider(name + " Q", createLinear(1, 100, 0.3f, 6.0f));
    }

    public void addStateVariable(String name, int low, int high) {
        addSlider(name + " Cutoff", createNone(low, high));
        addSlider(name + " Res", createLinear(70, 200, 0.7f, 2.0f));
    }

    public Object addMixerChannel(String name) {
        return addSlider(name, createLinear(0, 100, 0, 1));
    }

    public float getControlValue(int index) {
        return controls.get(index).getValue();
    }

    public void clear() {
        for(Controller c : controls) {
            cp5.remove(c.getName());
        }
        controls.clear();
        currentColumn = 0;
        currentRow = 0;
        if(module != null && module.ui instanceof UIForProcessing) {
            ((UIForProcessing) module.ui).removeExtraUI(cp5);
        }

    }

    public void hide() {
        noteNumberInput.hide();
        setNoteButton.hide();
        doubleFreqButton.hide();
        halveFreqButton.hide();
    }

    public void show() {
        noteNumberInput.show();
        setNoteButton.show();
        doubleFreqButton.show();
        halveFreqButton.show();
    }

    public void setPosition(int x, int y) {
        layout.leftMargin = x;
        layout.topMargin = y;
    }
}
