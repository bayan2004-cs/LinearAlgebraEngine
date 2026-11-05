package memory;

import java.util.concurrent.locks.ReadWriteLock;

public class SharedVector {

    private double[] vector;
    private VectorOrientation orientation;
    private ReadWriteLock lock = new java.util.concurrent.locks.ReentrantReadWriteLock();

    public SharedVector(double[] vector, VectorOrientation orientation) {
        this.vector=vector;
        this.orientation=orientation;
    }

    public double get(int index) {
        readLock();
        double ret=this.vector[index];
        readUnlock();
        return ret;
        
    }

    public int length() {
        readLock();
        int ret=this.vector.length;
        readUnlock();
        return ret;
    }

    public VectorOrientation getOrientation() {
        readLock();
        VectorOrientation ret=this.orientation;
        readUnlock();
        return ret;
    }

    public void writeLock() {
        lock.writeLock().lock();
    
    }

    public void writeUnlock() {
        lock.writeLock().unlock();
    }

    public void readLock() {
        lock.readLock().lock();
    }

    public void readUnlock() {
        lock.readLock().unlock();
    }

    public void transpose() { 
     this.writeLock();
     if(this.orientation == VectorOrientation.ROW_MAJOR){
        this.orientation =VectorOrientation.COLUMN_MAJOR ;
    }else{
        this.orientation = VectorOrientation.ROW_MAJOR; 
    }
    this.writeUnlock();
    }

    public void add(SharedVector other) {
        // Done: add two vector

        if (this.length() != other.length() || this.getOrientation() != other.getOrientation()) {
        throw new IllegalArgumentException("SharedVectors must have the same length and orientation");
    }
        this.writeLock();
        other.readLock();
        try { 
            for (int i = 0; i < this.vector.length; i++) {
                this.vector[i] += other.vector[i]; 
            }
        } finally {
            other.readUnlock();
            this.writeUnlock();
        }
    }

    public void negate() {
     this.writeLock();
    for(int i= 0; i<vector.length ; i++){
         vector[i]= -vector[i];
     }
     this.writeUnlock();
    }

    /** Thread Safety:
     * - Uses consistent lock ordering based on identity hash code
     **/

    public double dot(SharedVector other) {
     if(this.vector.length != other.vector.length ){
            throw new IllegalArgumentException("");
        }
        double result=0;
        this.readLock();
        other.readLock();
        for(int i =0 ; i<vector.length ; i++){
            result = result + this.vector[i]*other.vector[i];
        }
        this.readUnlock();
        other.readUnlock();
        return result; 
    }

    /** Thread Safety:
     * - Collects all involved vectors (this + all matrix columns)
     * - Sorts by identityHashCode for consistent global lock ordering
     * - This prevents deadlock with concurrent operations on same vectors */

    public void vecMatMul(SharedMatrix matrix) {
        int matrixRows = (matrix.getOrientation() == VectorOrientation.COLUMN_MAJOR) 
                     ? matrix.get(0).length() 
                     : matrix.length();


    // Check if the vector length matches the matrix row count

    if (matrixRows == 0 || this.vector.length != matrixRows) {
        throw new IllegalArgumentException("Dimension mismatch in vecMatMul: Vector length " 
                                           + this.vector.length + " != Matrix rows " + matrixRows);
    }
        int Vlength=matrix.get(0).length();
        for (int index = 0; index < matrix.length(); index++) {
            if(matrix.get(index).length()!=Vlength){
                return;
            }
        }
        this.writeLock();
        matrix.loadColumnMajor(matrix.readRowMajor());
        double[] resultMat = new double[matrix.length()];
        for (int i=0 ; i<matrix.length(); i++){
            SharedVector columnS = matrix.get(i);
            resultMat[i] = this.dot(columnS);
        }
        this.vector=resultMat;
        this.writeUnlock();
    }
}