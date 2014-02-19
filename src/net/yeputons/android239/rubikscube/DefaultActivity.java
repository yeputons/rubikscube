package net.yeputons.android239.rubikscube;

import android.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import min3d.core.RendererActivity;
import min3d.vos.Light;
import min3d.vos.Number3d;
import static junit.framework.Assert.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class DefaultActivity extends RendererActivity implements View.OnTouchListener, OnCubeRotationDoneListener {
    RubiksCube cube;

    @Override
    public void initScene() {
        cube = new RubiksCube();
        cube.setOnCubeRotationDoneListener(this);

        glSurfaceView().setOnTouchListener(this);

        scene.addChild(cube);
        scene.camera().position = new Number3d(4, 5, 8);
        scene.camera().target = new Number3d(0, 0, 0);
        scene.lights().add(new Light());
    }

    @Override
    protected void onCreateSetContentView()
    {
        setContentView(R.layout.main);
        LinearLayout main = (LinearLayout) findViewById(R.id.mainLayout);
        main.addView(_glSurfaceView);

        LinearLayout buttonsLayout = (LinearLayout) findViewById(R.id.buttonsLayout);
        final String[] names = {
                "Front",
                "Right",
                "Back",
                "Left",
                "Top",
                "Bottom"
        };
        for (int i = 0; i < 6; i++) {
            Button btn = new Button(this);
            btn.setText(names[i]);
            final int id = i;
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (cube.isRotationInProgress()) {
                        return;
                    }
                    cube.startRotation(id, +1);
                }
            });
            buttonsLayout.addView(btn);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, 0, 0, "Reset camera");
        menu.add(0, 1, 1, "Reset cube");
        menu.add(0, 2, 2, "Shuffle cube");
        menu.add(0, 4, 4, "Build cube");
        menu.add(0, 5, 5, "Save");
        menu.add(0, 6, 6, "Restore");
        return true;
    }

    int nearestAction = -1;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        nearestAction = item.getItemId();
        return true;
    }

    final Random rnd = new Random();
    ArrayList<Integer> sequence;

    @Override
    public void updateScene() {
        switch (nearestAction) {
            case 0:
                scene.camera().position = new Number3d(4, 5, 8);
                break;
            case 1:
                scene.removeChild(cube);
                cube = new RubiksCube();
                cube.setOnCubeRotationDoneListener(this);
                scene.addChild(cube);
                break;
            case 2:
                cube.stopRotation();
                for (int i = 0; i < 1000; i++) {
                    cube.performRotation(rnd.nextInt(6), rnd.nextInt(2) * 2 - 1);
                }
                cube.commitRotation();
                break;
            case 4:
                buildCube();
                onCubeRotationDone();
                break;
            case 5:
                try {
                    ObjectOutputStream out = new ObjectOutputStream(openFileOutput("cube.bin", MODE_PRIVATE));
                    out.writeObject(cube);
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
                break;
            case 6:
                try {
                    RubiksCube newCube = null;
                    ObjectInputStream in = new ObjectInputStream(openFileInput("cube.bin"));
                    try {
                        newCube  = (RubiksCube) in.readObject();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                    in.close();

                    if (newCube != null) {
                        scene.removeChild(cube);
                        cube = newCube;
                        cube.setOnCubeRotationDoneListener(this);
                        scene.addChild(cube);
                    }
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    new AlertDialog.Builder(this)
                            .setTitle("Unable to load cube")
                            .show();
                }
                break;
            case -1:
                break;
        }
        nearestAction = -1;

        cube.update();
    }

    float startX, startY;
    Number3d startPos;

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        System.out.println(motionEvent.getAction());
        switch (motionEvent.getAction()) {
        case MotionEvent.ACTION_DOWN:
            startX = motionEvent.getX();
            startY = motionEvent.getY();
            startPos = scene.camera().position.clone();
            return true;
        case MotionEvent.ACTION_MOVE:
            float diffX = -(motionEvent.getX() - startX) * 0.3f;
            float diffY = (motionEvent.getY() - startY) * 0.03f;
            scene.camera().position = startPos.clone();
            scene.camera().position.rotateY((float) (diffX * Math.PI / 180));
            scene.camera().position.y += diffY;
            return true;
        }
        return false;
    }

    void placeLeftCorner(SequenceRecorder cur) {
        cur.performRotation(RubiksCube.BOTTOM);
        cur.performRotation(RubiksCube.LEFT);
        cur.performRotation(RubiksCube.BOTTOM);
        cur.performRotation(RubiksCube.BOTTOM);
        cur.performRotation(RubiksCube.BOTTOM);
        cur.performRotation(RubiksCube.LEFT);
        cur.performRotation(RubiksCube.LEFT);
        cur.performRotation(RubiksCube.LEFT);
    }

    int seqPos;

    private void buildCube() {
        sequence = new ArrayList<Integer>();
        seqPos = 0;

        cube.stopRotation();
        SequenceRecorder cur = new SequenceRecorder(cube, sequence);
        buildLayer1(cur);
        assertTrue(cur.isIdentity());
        buildLayer2(cur);
        assertTrue(cur.isIdentity());
        buildLayer3(cur);
        assertTrue(cur.isIdentity());
    }

    private void buildLayer1(SequenceRecorder cur) {
        int topColor = cur.getColor(RubiksCube.TOP, 1, 1);
        // Building top cross
        for (;;) {
            boolean found = false;

            final int dx[] = { 0, 1, 1, 2 };
            final int dy[] = { 1, 0, 2, 1 };

            for (int i = 0; i < 4; i++, cur.rotateY()) {
                for (int i2 = 0; i2 < 4; i2++)
                    if (cur.getColor(RubiksCube.FRONT, dx[i2], dy[i2]) == topColor) {
                        found = true;
                        while (cur.getColor(RubiksCube.TOP, 1, 2) == topColor) {
                            cur.performRotation(RubiksCube.TOP);
                        }
                        while (cur.getColor(RubiksCube.FRONT, 0, 1) != topColor) {
                            cur.performRotation(RubiksCube.FRONT);
                        }
                        while (cur.getColor(RubiksCube.TOP, 0, 1) == topColor) {
                            cur.performRotation(RubiksCube.TOP);
                        }
                        assertEquals(topColor, cur.getColor(RubiksCube.FRONT, 0, 1));
                        cur.performRotation(RubiksCube.LEFT);
                        cur.performRotation(RubiksCube.LEFT);
                        cur.performRotation(RubiksCube.LEFT);
                        assertEquals(topColor, cur.getColor(RubiksCube.TOP, 0, 1));
                    }
            }

            {
                final int df[] = { RubiksCube.LEFT, RubiksCube.BACK, RubiksCube.FRONT, RubiksCube.RIGHT };
                for (int i = 0; i < 4; i++) {
                    if (cur.getColor(RubiksCube.BOTTOM, dx[i], dy[i]) == topColor) {
                        found = true;
                        while (cur.getColor(RubiksCube.TOP, dx[i], dy[i]) == topColor) {
                            cur.performRotation(RubiksCube.TOP);
                        }
                        cur.performRotation(df[i]);
                    }
                }
            }

            if (!found) break;
        }
        // Extending the cross
        for (;;) {
            boolean found = false;

            for (int i = 0; i < 4; i++, cur.rotateY()) {
                if (cur.getColor(RubiksCube.FRONT, 1, 2) != cur.getColor(RubiksCube.FRONT, 1, 1)) {
                    found = true;
                    cur.performRotation(RubiksCube.FRONT);
                    cur.performRotation(RubiksCube.FRONT);

                    int cnt = 0;
                    for (;;) {
                        while (cur.getColor(RubiksCube.FRONT, 1, 0) != cur.getColor(RubiksCube.FRONT, 1, 1)) {
                            cnt++;
                            cur.performRotation(RubiksCube.BOTTOM);
                            cur.rotateY();
                            cur.rotateY();
                            cur.rotateY();
                        }
                        cur.performRotation(RubiksCube.FRONT);
                        cur.performRotation(RubiksCube.FRONT);
                        if (cnt % 4 == 0) break;
                    }
                }
            }
            if (!found) break;
        }
        checkCross(cur);
        // Fixing the corners
        for (int state = 0; state < 3;) {
            boolean found = false;
            for (int t = 0; t < 2; t++, cur.flipVer())
            for (int i = 0; i < 4; i++, cur.rotateY()) {
                if (found) continue;
                if (cur.getColor(RubiksCube.FRONT, 0, 0) == topColor && state == 0) {
                    int was = 0;
                    while (cur.getColor(RubiksCube.BOTTOM, 0, 2) != cur.getColor(RubiksCube.FRONT, 1, 1)) {
                        cur.performRotation(RubiksCube.BOTTOM);
                        was++;
                        cur.rotateY();
                        cur.rotateY();
                        cur.rotateY();
                    }
                    placeLeftCorner(cur);
                    for (; was > 0; was--) {
                        cur.rotateY();
                    }
                    checkCross(cur);
                    found = true;
                }
                if (cur.getColor(RubiksCube.BOTTOM, 2, 2) == topColor && state == 1) {
                    cur.performRotation(RubiksCube.RIGHT);
                    cur.performRotation(RubiksCube.BOTTOM);
                    cur.performRotation(RubiksCube.BOTTOM);
                    cur.performRotation(RubiksCube.RIGHT);
                    cur.performRotation(RubiksCube.RIGHT);
                    cur.performRotation(RubiksCube.RIGHT);
                    checkCross(cur);
                    found = true;
                }
                if (state == 2) {
                    int[] expected = { topColor, cur.getColor(RubiksCube.FRONT, 1, 1), cur.getColor(RubiksCube.LEFT, 1, 1) };
                    int[] real = {
                            cur.getColor(RubiksCube.TOP, 0, 2),
                            cur.getColor(RubiksCube.FRONT, 0, 2),
                            cur.getColor(RubiksCube.LEFT, 2, 2)
                    };
                    if (!Arrays.equals(expected, real)) {
                        placeLeftCorner(cur);
                        checkCross(cur);
                        found = true;
                    }
                }
            }
            if (found) {
                state = 0;
            } else {
                state++;
            }
        }
        checkCross(cur);
    }

    private void placeRightSide(SequenceRecorder cur) {
        cur.performRotation(RubiksCube.BOTTOM);
        cur.performRotation(RubiksCube.BOTTOM);
        cur.performRotation(RubiksCube.BOTTOM);
        cur.performRotation(RubiksCube.RIGHT);
        cur.performRotation(RubiksCube.BOTTOM);
        cur.performRotation(RubiksCube.RIGHT);
        cur.performRotation(RubiksCube.RIGHT);
        cur.performRotation(RubiksCube.RIGHT);
        cur.rotateY();
        cur.rotateY();
        cur.rotateY();
        placeLeftCorner(cur);
        cur.rotateY();
        checkCross(cur);
    }

    private void buildLayer2(SequenceRecorder cur) {
        int topColor = cur.getColor(RubiksCube.TOP, 1, 1);
        int bottomColor = cur.getColor(RubiksCube.BOTTOM, 1, 1);
        for (int state = 0; state < 2;) {
            boolean found = false;
            for (int t = 0; t < 2; t++, cur.flipVer())
            for (int i = 0; i < 4; i++, cur.rotateY()) {
                if (found) continue;

                if (state == 0) {
                    int curColor = cur.getColor(RubiksCube.FRONT, 1, 0);
                    int secColor = cur.getColor(RubiksCube.BOTTOM, 1, 2);
                    if (curColor == topColor || curColor == bottomColor) continue;
                    if (secColor == topColor || secColor == bottomColor) continue;

                    int cnt = 0;
                    while (curColor != cur.getColor(RubiksCube.FRONT, 1, 1)) {
                        cnt++;
                        cur.rotateY();
                        cur.rotateY();
                        cur.rotateY();
                    }

                    if (cur.getColor(RubiksCube.RIGHT, 1, 1) == secColor) {
                        for (int i2 = 0; i2 < cnt; i2++)
                            cur.performRotation(RubiksCube.BOTTOM);
                        placeRightSide(cur);
                        found = true;
                    }

                    while (cnt > 0) {
                        cnt--;
                        cur.rotateY();
                    }
                }
                if (state == 1) {
                    int[] real = { cur.getColor(RubiksCube.FRONT, 2, 1), cur.getColor(RubiksCube.RIGHT, 2, 1) };
                    int[] expected = { cur.getColor(RubiksCube.FRONT, 1, 1), cur.getColor(RubiksCube.RIGHT, 1, 1) };
                    if (!Arrays.equals(real, expected)) {
                        placeRightSide(cur);
                        found = true;
                    }
                }
            }
            if (!found) {
                state++;
            } else {
                state = 0;
            }
        }
    }

    private void buildLayer3(SequenceRecorder cur) {
        final int bottomColor = cur.getColor(RubiksCube.BOTTOM, 1, 1);
        for (;;) {
            int col = cur.getColor(RubiksCube.BOTTOM, 1, 0);
            if (col == bottomColor) col = cur.getColor(RubiksCube.BACK, 1, 0);
            if (col == cur.getColor(RubiksCube.BACK, 1, 1)) break;
            cur.performRotation(RubiksCube.BOTTOM);
        }
        int l = cur.getColor(RubiksCube.LEFT, 1, 0), l0 = cur.getColor(RubiksCube.LEFT, 1, 1);
        if (l == bottomColor) l = cur.getColor(RubiksCube.BOTTOM, 0, 1);

        int f = cur.getColor(RubiksCube.FRONT, 1, 0), f0 = cur.getColor(RubiksCube.FRONT, 1, 1);
        if (f == bottomColor) f = cur.getColor(RubiksCube.BOTTOM, 1, 2);

        int r = cur.getColor(RubiksCube.RIGHT, 1, 0), r0 = cur.getColor(RubiksCube.RIGHT, 1, 1);
        if (r == bottomColor) r = cur.getColor(RubiksCube.BOTTOM, 2, 1);

        for (int step = 0; step < 3; step++) {
            if (f == r0) {
                swapLayer3Edges(cur);
                int tmp = f; f = r; r = tmp;
            } else if (l == r0 || l == f0) {
                cur.rotateY();
                swapLayer3Edges(cur);
                cur.rotateY();
                cur.rotateY();
                cur.rotateY();
                int tmp = f; f = l; l = tmp;
            }
        }
        assertEquals(l0, l);
        assertEquals(f0, f);
        assertEquals(r0, r);
    }

    private void swapLayer3Edges(SequenceRecorder cur) {
        cur.performRotation(RubiksCube.BOTTOM);
        cur.performRotation(RubiksCube.FRONT);
        cur.performRotation(RubiksCube.FRONT);
        cur.performRotation(RubiksCube.FRONT);
        cur.performRotation(RubiksCube.LEFT);
        cur.performRotation(RubiksCube.BOTTOM);
        cur.performRotation(RubiksCube.LEFT);
        cur.performRotation(RubiksCube.LEFT);
        cur.performRotation(RubiksCube.LEFT);
        cur.performRotation(RubiksCube.BOTTOM);
        cur.performRotation(RubiksCube.BOTTOM);
        cur.performRotation(RubiksCube.BOTTOM);
        cur.performRotation(RubiksCube.FRONT);
    }

    private void checkCross(SequenceRecorder cur) {
        int topColor = cur.getColor(RubiksCube.TOP, 1, 1);
        assertEquals(topColor, cur.getColor(RubiksCube.TOP, 0, 1));
        assertEquals(topColor, cur.getColor(RubiksCube.TOP, 1, 0));
        assertEquals(topColor, cur.getColor(RubiksCube.TOP, 2, 1));
        assertEquals(topColor, cur.getColor(RubiksCube.TOP, 1, 2));
        for (int i = 0; i < 4; i++) if (i != RubiksCube.TOP && i != RubiksCube.BOTTOM)
            assertEquals(cur.getColor(i, 1, 1), cur.getColor(i, 1, 2));
    }

    @Override
    public void onCubeRotationDone() {
        if (sequence == null || seqPos >= sequence.size()) return;

        int nextEq = 0;
        int face = -1;
        while (nextEq % 4 == 0 && seqPos < sequence.size()) {
            nextEq = 0;
            while (seqPos + nextEq < sequence.size()) {
                if (sequence.get(seqPos) == sequence.get(seqPos + nextEq)) {
                    nextEq++;
                } else {
                    break;
                }
            }
            face = sequence.get(seqPos);
            seqPos += nextEq;
        }

        switch (nextEq % 4) {
            case 0: break;
            case 2:
                seqPos--;
                cube.startRotation(face, 1);
                break;
            case 1: cube.startRotation(face,  1); break;
            case 3: cube.startRotation(face, -1); break;
        }
    }
}
