package net.yeputons.android239.rubikscube;

import java.util.ArrayList;

public class SequenceRecorder {
    private class OriginalFaceInfo implements Cloneable {
        public int originalFace;
        // We applied wasSwapped first, revA&revB then and got current face
        // A looks to the right (X)
        // B looks to the top (Y)
        public boolean wasSwapped, revA, revB;
        public boolean isReversed;

        public OriginalFaceInfo clone() {
            OriginalFaceInfo other = new OriginalFaceInfo();
            other.originalFace = originalFace;
            other.wasSwapped = wasSwapped;
            other.revA = revA;
            other.revB = revB;
            other.isReversed = isReversed;
            return other;
        }

        public void rotateCounterClockwise() {
            wasSwapped = !wasSwapped;
            {
                boolean neA = revB, neB = revA;
                revA = neA;
                revB = neB;
            }
            revB = !revB;
        }

        public void flipVer() {
            revA = !revA;
            isReversed = !isReversed;
        }
    }

    private RubiksCube cube;
    private ArrayList<Integer> sequence;
    private OriginalFaceInfo[] currentRotation;

    public SequenceRecorder(RubiksCube start, ArrayList<Integer> sequence) {
        cube = start.clone();
        this.sequence = sequence;
        currentRotation = new OriginalFaceInfo[6];
        for (int i = 0; i < 6; i++) {
            currentRotation[i] = new OriginalFaceInfo();
            currentRotation[i].originalFace = i;
        }

        rotateY();
        rotateY();
        rotateY();
        rotateY();
        for (int i = 0; i < 6; i++) {
            if (currentRotation[i].originalFace != i ||
             currentRotation[i].wasSwapped ||
             currentRotation[i].revA ||
             currentRotation[i].revB ||
                    currentRotation[i].isReversed)
                throw new AssertionError("Botva");
        }
    }

    public int getColor(int face, int a, int b) {
        OriginalFaceInfo info = currentRotation[face];
        if (info.revA) a = 2 - a;
        if (info.revB) b = 2 - b;
        if (info.wasSwapped) {
            int na = b, nb = a;
            a = na; b = nb;
        }
        return cube.getColor(info.originalFace, a, b);
    }
    public void performRotation(int face) {
        OriginalFaceInfo info = currentRotation[face];
        int cnt = info.isReversed ? 3 : 1;
        for (int i = 0; i < cnt; i++) {
            cube.performRotation(info.originalFace, 1);
            sequence.add(info.originalFace);
        }
    }

    public void flipVer() {
        {
            OriginalFaceInfo t = currentRotation[RubiksCube.LEFT];
            currentRotation[RubiksCube.LEFT] = currentRotation[RubiksCube.RIGHT];
            currentRotation[RubiksCube.RIGHT] = t;
            //currentRotation[RubiksCube.LEFT].isReversed = !currentRotation[RubiksCube.LEFT].isReversed;
            //currentRotation[RubiksCube.RIGHT].isReversed = !currentRotation[RubiksCube.RIGHT].isReversed;
        }
        for (int i = 0; i < 6; i++) if (i != RubiksCube.LEFT && i != RubiksCube.RIGHT) {
            currentRotation[i].flipVer();
        }
    }

    /**
     * Rotates your POV counter-clockwise (facing from the top)
     */
    public void rotateY() {
        OriginalFaceInfo[] old = new OriginalFaceInfo[6];
        for (int i = 0; i < 6; i++) {
            old[i] = currentRotation[i].clone();
        }
        currentRotation[RubiksCube.FRONT] = old[RubiksCube.LEFT];
        currentRotation[RubiksCube.LEFT] = old[RubiksCube.BACK];
        currentRotation[RubiksCube.BACK] = old[RubiksCube.RIGHT];
        currentRotation[RubiksCube.RIGHT] = old[RubiksCube.FRONT];

        currentRotation[RubiksCube.FRONT].isReversed = !currentRotation[RubiksCube.FRONT].isReversed;
        currentRotation[RubiksCube.BACK].isReversed = !currentRotation[RubiksCube.BACK].isReversed;

        currentRotation[RubiksCube.LEFT].revA = !currentRotation[RubiksCube.LEFT].revA;
        currentRotation[RubiksCube.RIGHT].revA = !currentRotation[RubiksCube.RIGHT].revA;

        currentRotation[RubiksCube.TOP].rotateCounterClockwise();
        currentRotation[RubiksCube.BOTTOM].rotateCounterClockwise();
    }
}
