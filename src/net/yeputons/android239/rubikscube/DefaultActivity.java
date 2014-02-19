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
                    this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(DefaultActivity.this)
                                    .setTitle("Unable to load cube")
                                    .show();
                        }
                    });
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

    int seqPos;
    int seqOps;

    private void buildCube() {
        sequence = new ArrayList<Integer>();
        seqPos = 0;
        seqOps = 0;

        cube.stopRotation();
        SequenceRecorder cur = new SequenceRecorder(cube, sequence);
        RubikSolver rubikSolver = new RubikSolver(cur);
        rubikSolver.buildLayer1();
        rubikSolver.buildLayer2();
        rubikSolver.buildLayer3();
        Log.d("rubikscube", String.format("%d operations before optimizations", sequence.size()));
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

        if (nextEq % 4 != 0) seqOps++;
        switch (nextEq % 4) {
            case 0: break;
            case 2:
                seqPos--;
                cube.startRotation(face, 1);
                break;
            case 1: cube.startRotation(face,  1); break;
            case 3: cube.startRotation(face, -1); break;
        }
        if (seqPos >= sequence.size()) {
            Log.d("rubikscube", String.format("%d operations after optimizations", seqOps));
        }
    }
}
