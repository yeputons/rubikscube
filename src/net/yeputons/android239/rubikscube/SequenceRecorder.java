package net.yeputons.android239.rubikscube;

import java.util.ArrayList;

public class SequenceRecorder {
    private RubiksCube cube;
    private ArrayList<Integer> sequence;

    public SequenceRecorder(RubiksCube start, ArrayList<Integer> sequence) {
        cube = start.clone();
    }
    public int getColor(int face, int a, int b) {
        return cube.getColor(face, a, b);
    }
    public void performRotation(int face) {
        cube.performRotation(face);
        sequence.add(face);
    }
}
