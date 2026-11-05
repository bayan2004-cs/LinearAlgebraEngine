package memory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestVector {

    @Test
    void testConstructor() {
        double[] d = { 1, 2, 3 };
        SharedVector v = new SharedVector(d, VectorOrientation.ROW_MAJOR);

        assertEquals(3, v.length());
        assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation());
        assertEquals(1.0, v.get(0));
    }

   @Test
    void testGet() {
        double[] d = { 10, 20 };
        SharedVector v = new SharedVector(d, VectorOrientation.ROW_MAJOR);
        assertEquals(20.0, v.get(1));
    }

    @Test
    void testAdd() {
        double[] d1 = { 1, 2 };
        double[] d2 = { 3, 4 };
        SharedVector v1 = new SharedVector(d1, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(d2, VectorOrientation.ROW_MAJOR);

        v1.add(v2);
        assertEquals(4.0, v1.get(0));
        assertEquals(6.0, v1.get(1));
    }

    @Test
    void testAddNeg() {
        double[] d1 = { -1, -2 };
        double[] d2 = { 1, 2 };
        SharedVector v1 = new SharedVector(d1, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(d2, VectorOrientation.ROW_MAJOR);

        v1.add(v2);
        assertEquals(0.0, v1.get(0));
    }

   @Test
    void testAddErr() {
        double[] d1 = { 1 };
        double[] d2 = { 1, 2 };
        SharedVector v1 = new SharedVector(d1, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(d2, VectorOrientation.ROW_MAJOR);

        assertThrows(IllegalArgumentException.class, () -> v1.add(v2));
    }

    @Test
    void testAddOrientErr() {
        double[] d = { 1 };
        SharedVector v1 = new SharedVector(d, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(d, VectorOrientation.COLUMN_MAJOR);

        assertThrows(IllegalArgumentException.class, () -> v1.add(v2));
    }

     @Test
    void testNegate() {
        double[] d = { 1, -1, 0 };
        SharedVector v = new SharedVector(d, VectorOrientation.ROW_MAJOR);
        v.negate();
        assertEquals(-1.0, v.get(0));
        assertEquals(1.0, v.get(1));
        assertEquals(0.0, v.get(2), 0.00000001);
    }

    @Test
    void testTranspose() {
        double[] d = { 1 };
        SharedVector v = new SharedVector(d, VectorOrientation.ROW_MAJOR);
        v.transpose();
        assertEquals(VectorOrientation.COLUMN_MAJOR, v.getOrientation());
        v.transpose();
        assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation());
    }

    @Test
    void testDot() {
        double[] d1 = { 1, 2 };
        double[] d2 = { 3, 4 };
        // 3 + 8 = 11
        SharedVector v1 = new SharedVector(d1, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(d2, VectorOrientation.ROW_MAJOR);

        assertEquals(11.0, v1.dot(v2));
    }

    @Test
    void testVecMatMul() {
        // v=[1,2], m=[[1,2],[3,4]]
        // [1*1+2*3, 1*2+2*4] = [7, 10]
        SharedVector v = new SharedVector(new double[] { 1, 2 }, VectorOrientation.ROW_MAJOR);
        SharedMatrix m = new SharedMatrix(new double[][] { { 1, 2 }, { 3, 4 } });

        v.vecMatMul(m);
        assertEquals(7.0, v.get(0));
        assertEquals(10.0, v.get(1));
    }

    @Test
    void testVecMatMulSizeChange() {
        // v=[1, 2, 3], m=[[1,2],[3,4],[5,6]] (3x2)
        // res = 1x2
        SharedVector v = new SharedVector(new double[] { 1, 2, 3 }, VectorOrientation.ROW_MAJOR);
        SharedMatrix m = new SharedMatrix(new double[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });

        v.vecMatMul(m);
        assertEquals(2, v.length());
        assertEquals(22.0, v.get(0)); // 1+6+15
    }

    @Test
    void testVecMatMulErr() {
        SharedVector v = new SharedVector(new double[] { 1, 2 }, VectorOrientation.ROW_MAJOR);
        SharedMatrix m = new SharedMatrix(new double[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } }); // 3x2, need 2 rows
        assertThrows(IllegalArgumentException.class, () -> v.vecMatMul(m));
    }

    @Test
    void testLocks() {
        SharedVector v = new SharedVector(new double[] { 1 }, VectorOrientation.ROW_MAJOR);
        v.readLock();
        v.get(0);
        v.readUnlock();

        v.writeLock();
        v.negate();
        v.writeUnlock();
    }

}