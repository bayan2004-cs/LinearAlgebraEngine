package spl;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.List;
import spl.lae.LinearAlgebraEngine;
import memory.SharedMatrix;
import memory.SharedVector;
import memory.VectorOrientation;
public class TestLAE {
    static VectorOrientation horizontal = VectorOrientation.ROW_MAJOR;
    static VectorOrientation vertical = VectorOrientation.COLUMN_MAJOR;

    @Nested
    @DisplayName("Testing task creation")
    class testCreateTasks {

        private LinearAlgebraEngine lae;
        private MockSharedMatrix mockLeft;
        private MockSharedMatrix mockRight;
        private MockSharedVector mockVector;

        @BeforeEach
        void setup() throws Exception {
            lae = new LinearAlgebraEngine(4);

            // Using mockups, as suggested on "Test-Driven Development" guide on Moodle
            // Creating "fake" matrices and vectors for testing
            mockVector = new MockSharedVector(5);
            mockLeft = new MockSharedMatrix(3, horizontal, mockVector);
            mockRight = new MockSharedMatrix(3, horizontal, mockVector);

            // Injecting mocks into LAE instance using reflection
            injectPrivateField(lae, "leftMatrix", mockLeft);
            injectPrivateField(lae, "rightMatrix", mockRight);
        }

        @Test
        @DisplayName("Test if createAddTasks produces the correct number of tasks")
        public void testCreateAddTasksCount() {
            // The actual method we test
            List<Runnable> tasks = lae.createAddTasks();

            // for a 3-row matrix, we expect 3 tasks
            assertEquals(3, tasks.size(), "Should create exactly one task per row.");
        }

        @Test
        @DisplayName("Test if the created task actually calls the add method")
        public void testAddTasksLogic() {
            List<Runnable> tasks = lae.createAddTasks();

            // Check that the task is an add operation by running it
            tasks.get(0).run();

            // Valdidating that the add method was called on the mock vector
            assertTrue(mockVector.wasAddCalled, "The task should have called the vector's add() method.");
        }

        // Helper function to inject private fields using reflection
        // (Allowed in forum discussion)
        private void injectPrivateField(Object target, String fieldName, Object value) throws Exception {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        }

         @Test
        @DisplayName("Negative Test: Matrix row count mismatch")
        public void testAddRowsMismatch() throws Exception {
            // Override setup with a mismatched matrix (2 rows instead of 3)
            MockSharedMatrix mismatchedRight = new MockSharedMatrix(2, horizontal, mockVector);
            injectPrivateField(lae, "rightMatrix", mismatchedRight);

            // Verify that LAE validates dimensions before scheduling
            assertThrows(IllegalArgumentException.class, () -> {
                lae.createAddTasks();
            }, "Should throw exception when row counts do not match.");
        }

        @Test
        @DisplayName("Negative Test: Column count (vector length) mismatch")
        public void testAddColsMismatch() throws Exception {
            // A shorter vector for the right matrix
            MockSharedVector shortVector = new MockSharedVector(2);
            MockSharedMatrix mismatchedRight = new MockSharedMatrix(3, horizontal, shortVector);
            injectPrivateField(lae, "rightMatrix", mismatchedRight);

            // Checking column count (vector length) mismatch
            assertThrows(IllegalArgumentException.class, () -> {
                lae.createAddTasks();
            }, "Should throw IllegalArgumentException when vector lengths (columns) don't match.");
        }

        @Test
        @DisplayName("Negative Test: Matrices are null")
        public void testAddNullMatrices() throws Exception {
            // Setting left matrix to null
            injectPrivateField(lae, "leftMatrix", null);

            // Checking null pointer handling
            assertThrows(NullPointerException.class, () -> {
                lae.createAddTasks();
            });
        }

        // NEGATE TESTS

        @Test
        @DisplayName("Positive Test: Negate task integrity")
        public void testNegateTasksLogic() {
            // Unary operations like Negate only use the left matrix
            List<Runnable> tasks = lae.createNegateTasks();

            assertEquals(3, tasks.size(), "Should create 3 negate tasks for 3 rows.");

            // Execute task and verify it calls the correct method
            tasks.get(0).run();
            assertTrue(mockVector.wasNegateCalled, "Task should trigger vector negation.");
        }

        @Test
        @DisplayName("Negative Test: Negate with null matrix")
        public void testNegateNullMatrix() throws Exception {
            // Injecting null for left matrix
            injectPrivateField(lae, "leftMatrix", null);

            assertThrows(NullPointerException.class, () -> {
                lae.createNegateTasks();
            }, "Engine should throw NullPointerException when the matrix reference is null.");
        }

        @Test
        @DisplayName("Negative Test: Negate with empty matrix (0 rows)")
        public void testNegateEmptyMatrix() throws Exception {
            // Create a mock matrix with 0 rows to represent an empty matrix
            MockSharedMatrix emptyMatrix = new MockSharedMatrix(0, horizontal, mockVector);
            injectPrivateField(lae, "leftMatrix", emptyMatrix);

            // Verify that an exception is thrown for an empty matrix
            assertThrows(IllegalArgumentException.class, () -> {
                lae.createNegateTasks();
            }, "Should throw IllegalArgumentException when attempting to negate an empty matrix.");
        }

        // TRANSPOSE TESTS

        @Test
        @DisplayName("Positive Test: Transpose task integrity")
        public void testTransposeTasksLogic() {
            // Transpose operations use the left matrix
            List<Runnable> tasks = lae.createTransposeTasks();

            // Every row should have a corresponding transpose task
            assertEquals(3, tasks.size(), "Should create 3 transpose tasks for 3 rows.");

            // Execute task and verify it calls the correct method
            tasks.get(0).run();
            assertTrue(mockVector.wasTransposeCalled, "Task should trigger vector transposition.");
        }

        @Test
        @DisplayName("Negative Test: Transpose with null matrix")
        public void testTransposeNullMatrix() throws Exception {
            // Injecting null into left matrix
            injectPrivateField(lae, "leftMatrix", null);

            // Ensuring NullPointerException is thrown
            assertThrows(NullPointerException.class, () -> {
                lae.createTransposeTasks();
            }, "Engine should throw NullPointerException when the matrix reference is null.");
        }

        @Test
        @DisplayName("Negative Test: Transpose with empty matrix (0 rows)")
        public void testTransposeEmptyMatrix() throws Exception {
            // Creating a Mock matrix with 0 rows
            MockSharedMatrix emptyMatrix = new MockSharedMatrix(0, horizontal, mockVector);
            injectPrivateField(lae, "leftMatrix", emptyMatrix);

            // Verifying that an IllegalArgumentException is thrown for an empty matrix
            assertThrows(IllegalArgumentException.class, () -> {
                lae.createTransposeTasks();
            }, "Should throw IllegalArgumentException when attempting to transpose an empty matrix.");
        }

        // MULTIPLY TESTS

        @Test
        @DisplayName("Positive Test: Multiply task integrity")
        public void testMultiplyTasksLogic() throws Exception {
            // Right matrix must be COLUMN_MAJOR for multiplication
            MockSharedMatrix matrixR = new MockSharedMatrix(5, vertical, mockVector);
            injectPrivateField(lae, "rightMatrix", matrixR);
            List<Runnable> tasks = lae.createMultiplyTasks();

            // Every row should have a corresponding multiply task
            assertEquals(3, tasks.size(), "Should create 3 multiply tasks for 3 rows.");

            // Making sure the task calls the correct method
            tasks.get(0).run();
            assertTrue(mockVector.wasVecMatMulCalled, "Task should trigger vector multiplication logic.");
        }

        @Test
        @DisplayName("Negative Test: Multiply with null matrix")
        public void testMultiplyNullMatrix() throws Exception {
            // Checking null for left matrix
            injectPrivateField(lae, "leftMatrix", null);
            assertThrows(NullPointerException.class, () -> {
                lae.createMultiplyTasks();
            }, "Should throw NPE when left matrix is null.");

            // Reset and inject null into right matrix
            MockSharedMatrix validMatrix = new MockSharedMatrix(3, horizontal, mockVector);
            injectPrivateField(lae, "leftMatrix", validMatrix);
            injectPrivateField(lae, "rightMatrix", null);

            assertThrows(NullPointerException.class, () -> {
                lae.createMultiplyTasks();
            }, "Should throw NPE when right matrix is null.");
        }

        @Test
        @DisplayName("Negative Test: Multiply with empty matrix")
        public void testMultiplyEmptyMatrix() throws Exception {
            MockSharedMatrix emptyMatrix = new MockSharedMatrix(0, horizontal, mockVector);
            MockSharedMatrix validMatrix = new MockSharedMatrix(3, horizontal, mockVector);

            // Case 1: left is empty
            injectPrivateField(lae, "leftMatrix", emptyMatrix);
            injectPrivateField(lae, "rightMatrix", validMatrix);
            assertThrows(IllegalArgumentException.class, () -> lae.createMultiplyTasks());

            // Case 2: right is empty
            injectPrivateField(lae, "leftMatrix", validMatrix);
            injectPrivateField(lae, "rightMatrix", emptyMatrix);
            assertThrows(IllegalArgumentException.class, () -> lae.createMultiplyTasks());
        }

        @Test
        @DisplayName("Negative Test: Multiply Dimension Mismatch")
        public void testMultiplyDimensionMismatch() throws Exception {
            
            //Creating matrices with incompatible dimensions
            MockSharedVector rowVector = new MockSharedVector(5);
            MockSharedVector colVector = new MockSharedVector(3);
            MockSharedMatrix matrixL = new MockSharedMatrix(2, horizontal, rowVector);
            MockSharedMatrix matrixR = new MockSharedMatrix(4, vertical, colVector);

            //Setting matrices in LAE
            injectPrivateField(lae, "leftMatrix", matrixL);
            injectPrivateField(lae, "rightMatrix", matrixR);

            //Checking for dimension mismatch exception
            assertThrows(IllegalArgumentException.class, () -> {
                lae.createMultiplyTasks();
            }, "Should throw IllegalArgumentException when Left.cols != Right.rows");
        }
    }

    // Mockup classes implemented as nested classes to avoid creating new files
    private static class MockSharedMatrix extends SharedMatrix {
        private int rowCount;
        private VectorOrientation orientation;
        private SharedVector vector;

        public MockSharedMatrix(int rowCount, VectorOrientation orientation, SharedVector vector) {
            super();
            this.rowCount = rowCount;
            this.orientation = orientation;
            this.vector = vector;
        }

        @Override
        public int length() {
            return rowCount;
        }

        @Override
        public SharedVector get(int index) {
            return vector;
        }

        @Override
        public VectorOrientation getOrientation() {
            return orientation;
        }
    }

    private static class MockSharedVector extends SharedVector {
        public boolean wasAddCalled = false;
        public boolean wasVecMatMulCalled = false;
        public boolean wasNegateCalled = false;
        public boolean wasTransposeCalled = false;

        public MockSharedVector(int length) {
                   super(new double[length], horizontal);
        }

        @Override
        public void add(SharedVector other) {
            this.wasAddCalled = true;
        }

        @Override
        public void vecMatMul(SharedMatrix matrix) {
            this.wasVecMatMulCalled = true;
        }

        @Override
        public void negate() {
            this.wasNegateCalled = true;
        }

        @Override
        public void transpose() {
            this.wasTransposeCalled = true;
        }

        @Override
        public int length() {
            return super.length();
        }
    }
}

