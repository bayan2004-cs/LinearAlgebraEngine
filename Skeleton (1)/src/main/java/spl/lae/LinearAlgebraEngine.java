package spl.lae;

import parser.*;
import memory.*;
import scheduling.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LinearAlgebraEngine {

    private SharedMatrix leftMatrix = new SharedMatrix();
    private SharedMatrix rightMatrix = new SharedMatrix();
    private TiredExecutor executor;

    public LinearAlgebraEngine(int numThreads) {
        // done: create executor with given thread count
        executor=new TiredExecutor(numThreads);
    }

    public ComputationNode run(ComputationNode computationRoot) {
        // done: resolve computation tree step by step until final matrix is produced
      computationRoot.associativeNesting();
      
    while (computationRoot.getNodeType() != ComputationNodeType.MATRIX) {
       ComputationNode resolNode = computationRoot.findResolvable();
       loadAndCompute(resolNode);
      double[][] result = leftMatrix.readRowMajor();
      resolNode.resolve(result);
    }
    try {
        executor.shutdown();// CRITICAL: Always shutdown executor, even if an exception occurs
                            // This prevents worker thread leaks
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    return computationRoot;
    }

    public void loadAndCompute(ComputationNode node) {
        // done: load operand matrices
        // done: create compute tasks & submit tasks to executor
        if (node == null) return;
    double[][] data1 = node.getChildren().get(0).getMatrix();
    leftMatrix.loadRowMajor(data1);

    if (node.getChildren().size() > 1) {
        double[][] data2 = node.getChildren().get(1).getMatrix();
        // Multiply requires the second matrix to be Column Major
        if (node.getNodeType() == ComputationNodeType.MULTIPLY) {
            rightMatrix.loadColumnMajor(data2);
        } else {
            rightMatrix.loadRowMajor(data2);
        }
    }
    List<Runnable> tasks;
    switch (node.getNodeType()) {
        case ADD:       tasks = createAddTasks(); break;
        case MULTIPLY: tasks = createMultiplyTasks(); break;
        case NEGATE:    tasks = createNegateTasks(); break;
        case TRANSPOSE: tasks = createTransposeTasks(); break;
        default: return;
    }
        executor.submitAll(tasks);
    }
   

    public List<Runnable> createAddTasks() {
        // done: return tasks that perform row-wise addition
        if (leftMatrix.readRowMajor() == null) throw new NullPointerException(); 
        if (leftMatrix.length() == 0) throw new IllegalArgumentException();
        if (leftMatrix.length() == 0 || rightMatrix.length() == 0) throw new IllegalArgumentException();
        if (leftMatrix.length() != rightMatrix.length() || leftMatrix.get(0).length() != rightMatrix.get(0).length()) {
           throw new IllegalArgumentException("Dimension mismatch");
        }
        List<Runnable> tasks=new ArrayList<>();
                    for (int i = 0; i < leftMatrix.length(); i++) {
                    final int tempi=i;
                    tasks.add(()->leftMatrix.get(tempi).add(rightMatrix.get(tempi)));
                }
            
        
        return tasks;
    }

    public List<Runnable> createMultiplyTasks() {
        // done: return tasks that perform row × matrix multiplication
     
if (leftMatrix.length() == 0 || rightMatrix.length() == 0) 
        throw new IllegalArgumentException("Matrix is empty");

    int leftCols = leftMatrix.get(0).length();
    int rightRows;

    // Correctly identify Rows based on orientation
    if (rightMatrix.getOrientation() == VectorOrientation.COLUMN_MAJOR) {
        rightRows = rightMatrix.get(0).length(); // Length of a column vector = number of rows
    } else {
        rightRows = rightMatrix.length(); // Number of row vectors = number of rows
    }

    if (leftCols != rightRows) {
        throw new IllegalArgumentException("Multiply dimension mismatch: " + leftCols + " != " + rightRows);
    }
            List<Runnable> tasks=new ArrayList<>();   
                
                    for (int i = 0; i < leftMatrix.length(); i++) {
                    final int tempi=i;
                    
                    tasks.add(()->leftMatrix.get(tempi).vecMatMul(rightMatrix));
                
                }
        return tasks;
    }

    public List<Runnable> createNegateTasks() {
        // done: return tasks that negate rows
        if (leftMatrix.readRowMajor() == null) throw new NullPointerException(); // Use the getter if needed
        if (leftMatrix.length() == 0) throw new IllegalArgumentException();
        List<Runnable> tasks=new ArrayList<>();     
        if (leftMatrix==null) {
              leftMatrix=rightMatrix;
         }
         leftMatrix.loadRowMajor(leftMatrix.readRowMajor());   
        for (int i = 0; i < leftMatrix.length(); i++) {
         final int tempi=i;
         tasks.add(()->leftMatrix.get(tempi).negate());
        }
        return tasks;
    }

   
    public List<Runnable> createTransposeTasks() {
        // done: return tasks that transpose rows
        if (leftMatrix.readRowMajor() == null) throw new NullPointerException(); // Use the getter if needed
        if (leftMatrix.length() == 0) throw new IllegalArgumentException();
        List<Runnable> tasks=new ArrayList<>();
         //SharedMatrix re;  
         if (leftMatrix==null) {
             leftMatrix=rightMatrix;
         }
         leftMatrix.loadRowMajor(leftMatrix.readRowMajor());    
        for (int i = 0; i < leftMatrix.length(); i++) {
            final int tempi=i;
            tasks.add(()->leftMatrix.get(tempi).transpose());
        }
                
        return tasks;
    }

    public String getWorkerReport() {
        // done: return summary of worker activity
        return executor.getWorkerReport();
    }
}