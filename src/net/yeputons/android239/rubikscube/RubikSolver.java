package net.yeputons.android239.rubikscube;

import junit.framework.Assert;

import java.util.Arrays;

public class RubikSolver {
    private SequenceRecorder cur;
    public RubikSolver(SequenceRecorder _cur) {
        cur = _cur;
    }

    private void placeLeftCorner() {
        cur.performRotation(RubiksCube.BOTTOM);
        cur.performRotation(RubiksCube.LEFT);
        cur.performRotationRev(RubiksCube.BOTTOM);
        cur.performRotationRev(RubiksCube.LEFT);
    }

    void buildLayer1() {
        int topColor = cur.getColor(RubiksCube.TOP, 1, 1);
        // Building top cross
        for (; ; ) {
            boolean found = false;

            final int dx[] = {0, 1, 1, 2};
            final int dy[] = {1, 0, 2, 1};

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
                        Assert.assertEquals(topColor, cur.getColor(RubiksCube.FRONT, 0, 1));
                        cur.performRotationRev(RubiksCube.LEFT);
                        Assert.assertEquals(topColor, cur.getColor(RubiksCube.TOP, 0, 1));
                    }
            }

            {
                final int df[] = {RubiksCube.LEFT, RubiksCube.BACK, RubiksCube.FRONT, RubiksCube.RIGHT};
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
        for (; ; ) {
            boolean found = false;

            for (int i = 0; i < 4; i++, cur.rotateY()) {
                if (cur.getColor(RubiksCube.FRONT, 1, 2) != cur.getColor(RubiksCube.FRONT, 1, 1)) {
                    found = true;
                    cur.performRotation(RubiksCube.FRONT);
                    cur.performRotation(RubiksCube.FRONT);

                    int cnt = 0;
                    for (; ; ) {
                        while (cur.getColor(RubiksCube.FRONT, 1, 0) != cur.getColor(RubiksCube.FRONT, 1, 1)) {
                            cnt++;
                            cur.performRotation(RubiksCube.BOTTOM);
                            cur.rotateYRev();
                        }
                        cur.performRotation(RubiksCube.FRONT);
                        cur.performRotation(RubiksCube.FRONT);
                        if (cnt % 4 == 0) break;
                    }
                }
            }
            if (!found) break;
        }
        checkCross();
        // Fixing the corners
        for (int state = 0; state < 3; ) {
            boolean found = false;
            for (int t = 0; t < 2; t++, cur.flipVer())
                for (int i = 0; i < 4; i++, cur.rotateY()) {
                    if (found) continue;
                    if (cur.getColor(RubiksCube.FRONT, 0, 0) == topColor && state == 0) {
                        int was = 0;
                        while (cur.getColor(RubiksCube.BOTTOM, 0, 2) != cur.getColor(RubiksCube.FRONT, 1, 1)) {
                            cur.performRotation(RubiksCube.BOTTOM);
                            was++;
                            cur.rotateYRev();
                        }
                        placeLeftCorner();
                        for (; was > 0; was--) {
                            cur.rotateY();
                        }
                        checkCross();
                        found = true;
                    }
                    if (cur.getColor(RubiksCube.BOTTOM, 2, 2) == topColor && state == 1) {
                        cur.performRotation(RubiksCube.RIGHT);
                        cur.performRotation(RubiksCube.BOTTOM);
                        cur.performRotation(RubiksCube.BOTTOM);
                        cur.performRotationRev(RubiksCube.RIGHT);
                        checkCross();
                        found = true;
                    }
                    if (state == 2) {
                        int[] expected = {topColor, cur.getColor(RubiksCube.FRONT, 1, 1), cur.getColor(RubiksCube.LEFT, 1, 1)};
                        int[] real = {
                                cur.getColor(RubiksCube.TOP, 0, 2),
                                cur.getColor(RubiksCube.FRONT, 0, 2),
                                cur.getColor(RubiksCube.LEFT, 2, 2)
                        };
                        if (!Arrays.equals(expected, real)) {
                            placeLeftCorner();
                            checkCross();
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
        checkCross();
    }

    private void placeRightSide() {
        cur.performRotationRev(RubiksCube.BOTTOM);
        cur.performRotation(RubiksCube.RIGHT);
        cur.performRotation(RubiksCube.BOTTOM);
        cur.performRotationRev(RubiksCube.RIGHT);
        cur.rotateYRev();
        placeLeftCorner();
        cur.rotateY();
        checkCross();
    }

    void buildLayer2() {
        int topColor = cur.getColor(RubiksCube.TOP, 1, 1);
        int bottomColor = cur.getColor(RubiksCube.BOTTOM, 1, 1);
        for (int state = 0; state < 2; ) {
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
                            cur.rotateYRev();
                        }

                        if (cur.getColor(RubiksCube.RIGHT, 1, 1) == secColor) {
                            for (int i2 = 0; i2 < cnt; i2++)
                                cur.performRotation(RubiksCube.BOTTOM);
                            placeRightSide();
                            found = true;
                        }

                        while (cnt > 0) {
                            cnt--;
                            cur.rotateY();
                        }
                    }
                    if (state == 1) {
                        int[] real = {cur.getColor(RubiksCube.FRONT, 2, 1), cur.getColor(RubiksCube.RIGHT, 2, 1)};
                        int[] expected = {cur.getColor(RubiksCube.FRONT, 1, 1), cur.getColor(RubiksCube.RIGHT, 1, 1)};
                        if (!Arrays.equals(real, expected)) {
                            placeRightSide();
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

    private boolean isRightCornerOk() {
        int[] a = {
                cur.getColor(RubiksCube.RIGHT, 2, 0),
                cur.getColor(RubiksCube.FRONT, 2, 0),
                cur.getColor(RubiksCube.BOTTOM, 2, 2)
        };
        int[] b = {
                cur.getColor(RubiksCube.RIGHT, 1, 1),
                cur.getColor(RubiksCube.FRONT, 1, 1),
                cur.getColor(RubiksCube.BOTTOM, 1, 1)
        };
        Arrays.sort(a);
        Arrays.sort(b);
        return Arrays.equals(a, b);
    }

    void buildLayer3() {
        final int bottomColor = cur.getColor(RubiksCube.BOTTOM, 1, 1);
        for (; ; ) {
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
                swapLayer3Edges();
                int tmp = f;
                f = r;
                r = tmp;
            } else if (l == r0 || l == f0) {
                cur.rotateY();
                swapLayer3Edges();
                cur.rotateYRev();
                int tmp = f;
                f = l;
                l = tmp;
            }
        }
        Assert.assertEquals(l0, l);
        Assert.assertEquals(f0, f);
        Assert.assertEquals(r0, r);

        for (int i = 0; i < 4; i++, cur.performRotation(RubiksCube.BOTTOM)) {
            if (cur.getColor(RubiksCube.FRONT, 1, 0) != bottomColor)
                continue;
            for (int step = 0; step < 4; step++) {
                cur.performRotationRev(RubiksCube.FRONT);
                cur.performRotation(RubiksCube.TOP);
                cur.performRotation(RubiksCube.BOTTOM);
                cur.rotateYRev();
            }
        }

        for (; ; ) {
            boolean found = false;
            for (int i = 0; i < 4; i++, cur.rotateY())
                if (!found) {
                    cur.flipVer();
                    if (!isRightCornerOk()) {
                        cur.flipVer();
                        continue;
                    }
                    cur.flipVer();

                    found = true;
                    int ops = 0;
                    while (!isRightCornerOk()) {
                        shiftLayer3Corners();
                        ops++;
                        Assert.assertTrue(ops < 3);
                    }
                }
            if (found) break;
            shiftLayer3Corners();
        }

        for (int i = 0; i < 4; i++, cur.performRotation(RubiksCube.BOTTOM)) {
            while (cur.getColor(RubiksCube.BOTTOM, 0, 2) != bottomColor) {
                cur.performRotation(RubiksCube.LEFT);
                cur.performRotation(RubiksCube.FRONT);
                cur.performRotationRev(RubiksCube.LEFT);
                cur.performRotationRev(RubiksCube.FRONT);
            }
        }
    }

    private void shiftLayer3Corners() {
        cur.performRotation(RubiksCube.LEFT);
        cur.performRotation(RubiksCube.RIGHT);
        cur.performRotation(RubiksCube.BOTTOM);
        cur.performRotationRev(RubiksCube.LEFT);
        cur.performRotationRev(RubiksCube.BOTTOM);
        cur.performRotationRev(RubiksCube.RIGHT);
        placeLeftCorner();
    }

    private void swapLayer3Edges() {
        cur.performRotation(RubiksCube.BOTTOM);
        cur.performRotationRev(RubiksCube.FRONT);
        cur.performRotation(RubiksCube.LEFT);
        cur.performRotation(RubiksCube.BOTTOM);
        cur.performRotationRev(RubiksCube.LEFT);
        cur.performRotationRev(RubiksCube.BOTTOM);
        cur.performRotation(RubiksCube.FRONT);
    }

    private void checkCross() {
        int topColor = cur.getColor(RubiksCube.TOP, 1, 1);
        Assert.assertEquals(topColor, cur.getColor(RubiksCube.TOP, 0, 1));
        Assert.assertEquals(topColor, cur.getColor(RubiksCube.TOP, 1, 0));
        Assert.assertEquals(topColor, cur.getColor(RubiksCube.TOP, 2, 1));
        Assert.assertEquals(topColor, cur.getColor(RubiksCube.TOP, 1, 2));
        for (int i = 0; i < 4; i++)
            if (i != RubiksCube.TOP && i != RubiksCube.BOTTOM)
                Assert.assertEquals(cur.getColor(i, 1, 1), cur.getColor(i, 1, 2));
    }
}