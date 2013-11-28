package net.yeputons.android239.rubikscube;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import min3d.core.RendererActivity;
import min3d.objectPrimitives.Box;
import min3d.vos.Color4;
import min3d.vos.Light;
import min3d.vos.LightType;
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
                    cube.performRotation(rnd.nextInt(6));
                }
                cube.commitRotation();
                break;
            case 3:
                cube.stopRotation();
                cube.startRotation(rnd.nextInt(6));
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

    int seqPos;

    private void buildCube() {
        sequence = new ArrayList<Integer>();
        seqPos = 0;

        cube.stopRotation();
        final int topColor = cube.getColor(RubiksCube.TOP, 1, 1);
        SequenceRecorder cur = new SequenceRecorder(cube, sequence);
        for (;;) {
            boolean found = false;

            {
                final int dx[] = { 0, 1, 1, 2 };
                final int dy[] = { 1, 0, 2, 1 };
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
    }

    @Override
    public void onCubeRotationDone() {
        if (sequence == null || seqPos >= sequence.size()) return;
        cube.startRotation(sequence.get(seqPos));
        seqPos++;
    }
}
