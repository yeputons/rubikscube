package net.yeputons.android239.rubikscube;

import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import min3d.core.RendererActivity;
import min3d.vos.Light;
import min3d.vos.Number3d;

import java.util.ArrayList;
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
        menu.add(0, 3, 3, "Random move");
        menu.add(0, 4, 4, "Build cube");
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
            case 3:
                cube.stopRotation();
                cube.startRotation(rnd.nextInt(6), rnd.nextInt(2) * 2 - 1);
                break;
            case 4:
                buildCube();
                onCubeRotationDone();
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
        final int topColor = cube.getColor(RubiksCube.TOP, 1, 1);
        SequenceRecorder cur = new SequenceRecorder(cube, sequence);
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
                        if (cur.getColor(RubiksCube.FRONT, 0, 1) != topColor)
                            throw new AssertionError("Botva2");
                        cur.performRotation(RubiksCube.LEFT);
                        cur.performRotation(RubiksCube.LEFT);
                        cur.performRotation(RubiksCube.LEFT);
                        if (cur.getColor(RubiksCube.TOP, 0, 1) != topColor)
                            throw new AssertionError("Botva2");
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
                if (cur.getColor(RubiksCube.TOP, 0, 2) == topColor && state == 2
                        && (cur.getColor(RubiksCube.FRONT, 0, 2) != cur.getColor(RubiksCube.FRONT, 1, 1) ||
                            cur.getColor(RubiksCube.LEFT, 2, 2) != cur.getColor(RubiksCube.LEFT, 1, 1))) {
                    placeLeftCorner(cur);
                    checkCross(cur);
                    found = true;
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

    private void checkCross(SequenceRecorder cur) {
        int topColor = cur.getColor(RubiksCube.TOP, 1, 1);
        if (cur.getColor(RubiksCube.TOP, 0, 1) != topColor)
            throw new AssertionError("Botva3");
        if (cur.getColor(RubiksCube.TOP, 2, 1) != topColor)
            throw new AssertionError("Botva3");
        if (cur.getColor(RubiksCube.TOP, 1, 0) != topColor)
            throw new AssertionError("Botva3");
        if (cur.getColor(RubiksCube.TOP, 1, 2) != topColor)
            throw new AssertionError("Botva3");
        if (cur.getColor(RubiksCube.FRONT, 1, 2) != cur.getColor(RubiksCube.FRONT, 1, 1))
            throw new AssertionError("Botva4");
        if (cur.getColor(RubiksCube.BACK, 1, 2) != cur.getColor(RubiksCube.BACK, 1, 1))
            throw new AssertionError("Botva4");
        if (cur.getColor(RubiksCube.LEFT, 1, 2) != cur.getColor(RubiksCube.LEFT, 1, 1))
            throw new AssertionError("Botva4");
        if (cur.getColor(RubiksCube.RIGHT, 1, 2) != cur.getColor(RubiksCube.RIGHT, 1, 1))
            throw new AssertionError("Botva4");
    }

    @Override
    public void onCubeRotationDone() {
        if (sequence == null || seqPos >= sequence.size()) return;

        int nextEq = 0;
        int face = -1;
        while (nextEq % 4 == 0 && seqPos < sequence.size()) {
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
