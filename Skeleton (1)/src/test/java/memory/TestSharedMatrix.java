package memory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestSharedMatrix {

    @Test
    void testEmptyConstructor() {
        SharedMatrix m = new SharedMatrix();
        assertEquals(0, m.length());
    }

   @Test
   void testArrayConstructor() {
        double[][] data = { { 1.0, 2.0 }, { 3.0, 4.0 } };
        SharedMatrix m = new SharedMatrix(data);

        assertEquals(2, m.length());
        assertEquals(VectorOrientation.ROW_MAJOR, m.getOrientation());
    }

    @Test
    void testLoadRow() {
        SharedMatrix m = new SharedMatrix();
        double[][] d = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
        m.loadRowMajor(d);

        assertEquals(3, m.length());
        assertEquals(VectorOrientation.ROW_MAJOR, m.getOrientation());
    }

    @Test
    void testOneCol() {
        SharedMatrix m = new SharedMatrix();
        double[][] d = { { 1 }, { 2 }, { 3 } };
        m.loadRowMajor(d);
        assertEquals(3, m.length());
        assertEquals(1, m.get(0).length());
    }

    @Test
    void testLoadCol() {
        SharedMatrix m = new SharedMatrix();
        double[][] d = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
        m.loadColumnMajor(d);

        assertEquals(2, m.length()); // cols
        assertEquals(VectorOrientation.COLUMN_MAJOR, m.getOrientation());

        // col 1: 1, 3, 5
        assertEquals(1.0, m.get(0).get(0));
        assertEquals(5.0, m.get(0).get(2));
    }

    @Test
    void testReadRow() {
        double[][] d = { { 1, 2 }, { 3, 4 } };
        SharedMatrix m = new SharedMatrix(d);

        double[][] res = m.readRowMajor();
        assertEquals(1.0, res[0][0]);
        assertEquals(4.0, res[1][1]);
    }

     @Test
    void testReadRowFromCol() {
        SharedMatrix m = new SharedMatrix();
        double[][] d = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
        m.loadColumnMajor(d);

        double[][] res = m.readRowMajor();
        assertEquals(3, res.length);
        assertEquals(2, res[0].length);
        assertEquals(1.0, res[0][0]);
        assertEquals(6.0, res[2][1]);
    }

    @Test
     void testGet() {
        double[][] d = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
        SharedMatrix m = new SharedMatrix(d);
        assertEquals(1.0, m.get(0).get(0));
        assertEquals(5.0, m.get(2).get(0));
    }

    @Test
    void testOrientation() {
        double[][] d = { { 1 } };
        SharedMatrix m = new SharedMatrix(d);
        assertEquals(VectorOrientation.ROW_MAJOR, m.getOrientation());

        m.loadColumnMajor(d);
        assertEquals(VectorOrientation.COLUMN_MAJOR, m.getOrientation());
    }

     @Test
    void testAdd() {
        // verify locks usage
        double[][] d1 = { { 1, 2 }, { 3, 4 } };
        double[][] d2 = { { 5, 6 }, { 7, 8 } };
        SharedMatrix m1 = new SharedMatrix(d1);
        SharedMatrix m2 = new SharedMatrix(d2);

        for (int i = 0; i < m1.length(); i++) {
            m1.get(i).writeLock();
            m2.get(i).readLock();
            try {
                m1.get(i).add(m2.get(i));
            } finally {
                m1.get(i).writeUnlock();
                m2.get(i).readUnlock();
            }
        }

        double[][] res = m1.readRowMajor();
        assertEquals(6.0, res[0][0]);
        assertEquals(12.0, res[1][1]);
    }

    @Test
    void testNegate() {
        double[][] d = { { 1, 2 }, { 3, 4 } };
        SharedMatrix m = new SharedMatrix(d);

        for (int i = 0; i < m.length(); i++) {
            m.get(i).writeLock();
            try {
                m.get(i).negate();
            } finally {
                m.get(i).writeUnlock();
            }
        }

        assertEquals(-1.0, m.get(0).get(0));
    }
 
    @Test
    void testNonSquare() {
        double[][] d = { { 1, 2, 3 }, { 4, 5, 6 } };
        SharedMatrix m = new SharedMatrix(d);
        assertEquals(2, m.length());
        assertEquals(3, m.get(0).length());
    }

    @Test
    void testZeros() {
        double[][] d = { { 0, 0 } };
        SharedMatrix m = new SharedMatrix(d);
        assertEquals(0.0, m.get(0).get(0));
    }

    @Test
    void testNegatives() {
        double[][] d = { { -1, -2 } };
        SharedMatrix m = new SharedMatrix(d);
        assertEquals(-1.0, m.get(0).get(0));
    }

     @Test
    void testReload() {
        double[][] d1 = { { 1 } };
        SharedMatrix m = new SharedMatrix(d1);
        double[][] d2 = { { 1, 2 }, { 3, 4 } };
        m.loadRowMajor(d2);
        assertEquals(2, m.length());
    }
}