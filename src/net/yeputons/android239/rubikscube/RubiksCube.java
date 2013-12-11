package net.yeputons.android239.rubikscube;

import min3d.core.Object3dContainer;
import min3d.objectPrimitives.Box;
import min3d.vos.Color4;
import min3d.vos.Light;
import min3d.vos.Number3d;

import java.util.Random;

public class RubiksCube extends Object3dContainer implements Cloneable {
    public static final Color4 COLORS[] = {
            new Color4(255, 0, 0, 255),
            new Color4(0, 0, 255, 255),
            new Color4(255, 128, 0, 255),
            new Color4(0, 255, 0, 255),
            new Color4(255, 255, 255, 255),
            new Color4(255, 255, 0, 255)
    };

    public static final int FRONT = 0;
    public static final int RIGHT = 1;
    public static final int BACK = 2;
    public static final int LEFT = 3;
    public static final int TOP = 4;
    public static final int BOTTOM = 5;

    protected int[][][] cols;
    protected int[][][] facesMsk;
    protected Box[][][] boxes;
    private OnCubeRotationDoneListener listener;

    protected int curTurnFace = -1;
    protected int curTurnProgress = 90;
    protected int curTurnDirection = 0;

    public RubiksCube() {
        super();

        cols = new int[6][3][3];
        for (int i = 0; i < 6; i++)
            for (int y = 0; y < 3; y++)
                for (int x = 0; x < 3; x++)
                    cols[i][y][x] = i;
        boxes = new Box[3][3][3];

        recreateBoxes();
    }

    @Override
    public RubiksCube clone() {
        if (curTurnFace >= 0) {
            throw new RotationIsInProgressException();
        }

        RubiksCube other = new RubiksCube();
        for (int i = 0; i < 6; i++)
            for (int y = 0; y < 3; y++)
                for (int x = 0; x < 3; x++)
                    other.cols[i][y][x] = cols[i][y][x];
        other.recreateBoxes();
        return other;
    }

    public void setOnCubeRotationDoneListener(OnCubeRotationDoneListener listener) {
        this.listener = listener;
    }

    private void recreateBoxes() {
        synchronized (this) {
            for (int z = 0; z < 3; z++)
                for (int y = 0; y < 3; y++)
                    for (int x = 0; x < 3; x++)
                        if (boxes[z][y][x] != null)
                            removeChild(boxes[z][y][x]);

            Color4[][][][] rcols = new Color4[3][3][3][6];
            for (int z = 0; z < 3; z++)
                for (int y = 0; y < 3; y++)
                    for (int x = 0; x < 3; x++)
                        for (int i = 0; i < 6; i++)
                            rcols[z][y][x][i] = new Color4(128, 128, 128, 255);

            facesMsk = new int[3][3][3];
            for (int i = 0; i < 6; i++)
                for (int b = 0; b < 3; b++)
                    for (int a = 0; a < 3; a++) {
                        int x = i == LEFT ? 0 : i == RIGHT ? 2 : a;
                        int y = i == BOTTOM ? 0 : i == TOP ? 2 : b;
                        int z = i == BACK ? 0 : i == FRONT ? 2 : (i == BOTTOM || i == TOP ? b : a);
                        facesMsk[z][y][x] |= 1 << i;
                        rcols[z][y][x][i] = COLORS[cols[i][b][a]];
                    }

            for (int z = 0; z < 3; z++)
                for (int y = 0; y < 3; y++)
                    for (int x = 0; x < 3; x++) {
                        Box b = new Box(1, 1, 1, rcols[z][y][x]);
                        b.normalsEnabled(true);
                        b.colorMaterialEnabled(true);
                        b.position().setAll(
                                (x - 1) * 1.05f,
                                (y - 1) * 1.05f,
                                (z - 1) * 1.05f
                        );
                        boxes[z][y][x] = b;
                        addChild(b);
                    }
        }
    }

    public void startRotation(int face, int direction) {
        if (curTurnFace >= 0) {
            throw new RotationIsInProgressException();
        }
        if (direction != 1 && direction != -1) {
            throw new IllegalArgumentException("direction should be either -1 or +1");
        }
        curTurnFace = face;
        curTurnDirection = direction;
        curTurnProgress = 0;
    }
    public void stopRotation() {
        if (curTurnFace < 0) return;
        curTurnFace = -1;
        recreateBoxes();
    }

    public void performRotation(int face, int direction) {
        if (curTurnFace >= 0) {
            throw new RotationIsInProgressException();
        }
        if (direction != 1 && direction != -1) {
            throw new IllegalArgumentException("direction should be either -1 or +1");
        }
        if (direction == -1) {
            for (int i = 0; i < 3; i++)
                performRotation(face, 1);
            return;
        }
        rotateFace(cols[face], face == FRONT || face == BACK ? 1 : -1);
        if (face == LEFT || face == RIGHT)
            rotateBorderX(face == LEFT ? 0 : 2);
        if (face == TOP || face == BOTTOM)
            rotateBorderY(face == BOTTOM ? 0 : 2);
        if (face == FRONT || face == BACK)
            rotateBorderZ(face == BACK ? 0 : 2);
    }
    public void commitRotation() {
        recreateBoxes();
    }

    public int getColor(int face, int a, int b) {
        return cols[face][b][a];
    }

    public void update() {
        if (curTurnProgress >= 90) {
            if (curTurnFace >= 0) {
                int tmp = curTurnFace;
                curTurnFace = -1;
                performRotation(tmp, curTurnDirection);
                commitRotation();
                if (listener != null) {
                    listener.onCubeRotationDone();
                }
            }
            return;
        }

        for (int z = 0; z < 3; z++)
            for (int y = 0; y < 3; y++)
                for (int x = 0; x < 3; x++) {
                    if ((facesMsk[z][y][x] & (1 << curTurnFace)) != 0) {
                        float rotX = 0, rotY = 0, rotZ = 0;
                        if (curTurnFace == RIGHT || curTurnFace == LEFT)
                            rotX = 2 * curTurnDirection;
                        if (curTurnFace == TOP || curTurnFace == BOTTOM)
                            rotY = 2 * curTurnDirection;
                        if (curTurnFace == FRONT || curTurnFace == BACK)
                            rotZ = 2 * curTurnDirection;
                        boxes[z][y][x].position().rotateX((float) (rotX * Math.PI / 180));
                        boxes[z][y][x].position().rotateY((float) (rotY * Math.PI / 180));
                        boxes[z][y][x].position().rotateZ((float) (rotZ * Math.PI / 180));

                        boxes[z][y][x].rotation().x += rotX;
                        boxes[z][y][x].rotation().y += rotY;
                        boxes[z][y][x].rotation().z += rotZ;
                    }
                }
        curTurnProgress += 2;
    }

    private void rotateBorderX(int x) {
        for (int i = 0; i < 3; i++) {
            int[] old = {
                    cols[FRONT][i][x],
                    cols[TOP][2 - i][x],
                    cols[BACK][2 - i][x],
                    cols[BOTTOM][i][x]
            };
            cols[FRONT][i][x] = old[1];
            cols[TOP][2 - i][x] = old[2];
            cols[BACK][2 - i][x] = old[3];
            cols[BOTTOM][i][x] = old[0];
        }
    }

    private void rotateBorderY(int y) {
        for (int i = 0; i < 3; i++) {
            int[] old = {
                    cols[FRONT][y][i],
                    cols[LEFT][y][i],
                    cols[BACK][y][2 - i],
                    cols[RIGHT][y][2 - i],
            };
            cols[FRONT][y][i] = old[1];
            cols[LEFT][y][i] = old[2];
            cols[BACK][y][2 - i] = old[3];
            cols[RIGHT][y][2 - i] = old[0];
        }
    }

    private void rotateBorderZ(int z) {
        for (int i = 0; i < 3; i++) {
            int[] old = {
                    cols[RIGHT][i][z],
                    cols[BOTTOM][z][i],
                    cols[LEFT][2 - i][z],
                    cols[TOP][z][2 - i],
            };
            cols[RIGHT][i][z] = old[1];
            cols[BOTTOM][z][i] = old[2];
            cols[LEFT][2 - i][z] = old[3];
            cols[TOP][z][2 - i] = old[0];
        }
    }

    private void rotateFace(int[][] col, int dir) {
        if (dir != 1 && dir != -1)
            throw new IllegalArgumentException("Dir should be either -1 or +1");
        if (dir == -1) {
            rotateFace(col, +1);
            rotateFace(col, +1);
            rotateFace(col, +1);
            return;
        }
        int[][] ncol = new int[3][3];
        for (int y = 0; y < 3; y++)
            for (int x = 0; x < 3; x++)
                ncol[x][2 - y] = col[y][x];
        for (int y = 0; y < 3; y++)
            for (int x = 0; x < 3; x++)
                col[y][x] = ncol[y][x];
    }
}
