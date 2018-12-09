package com.korisnamedia.thonk.tuning;

public class NoteMapper {

    private double[] pitchMap = new double[128];

    private double c4 = 261.63;

    public NoteMapper() {
        for(int i=0;i<128;i++)
        {
            pitchMap[i] = Math.pow(2.0, (i - 60) / 12.0);
        }
    }
    public double getFrequency(int noteNumber) {

        return pitchMap[noteNumber] * c4;
    }

    public double getNote(double frequency) {
        double note = 60 + 12 * (Math.log(frequency / c4) / Math.log(2));
        return note;
    }
}
