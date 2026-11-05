package memory;

public class SharedMatrix {

    private volatile SharedVector[] vectors = {}; // underlying vectors

    public SharedMatrix() {
        vectors=new SharedVector[0];
    }

    public SharedMatrix(double[][] matrix) {
        SharedVector[] result=new SharedVector[matrix.length];
        for (int i = 0; i < result.length; i++) {
            SharedVector nS=new SharedVector(matrix[i], VectorOrientation.ROW_MAJOR);
            result[i]=nS;
        }
        this.vectors=result;
        
    }
    public void loadRowMajor(double[][] matrix) {
        
        int MatRow = matrix.length;
        SharedVector[] ret = new SharedVector[MatRow];
        for(int i=0; i<MatRow;i++){
            ret[i] = new SharedVector(matrix[i],VectorOrientation.ROW_MAJOR);
        }
        this.vectors=ret;
    }

    public void loadColumnMajor(double[][] matrix) {
            int numVectors = matrix.length; 
            int newRows = matrix[0].length;
            int newCols = numVectors;
            double[][] result = new double[newRows][newCols];
            SharedVector[] ret=new SharedVector[newRows];

            for (int i = 0; i < newRows; i++) {
                for (int j = 0; j < newCols; j++) {
                    result[i][j] = matrix[j][i];
                }
                ret[i]=new SharedVector(result[i], VectorOrientation.COLUMN_MAJOR);
            }
            
            this.vectors=ret;
            
    }

    public double[][] readRowMajor() {
           SharedVector[] ve=this.vectors;
           // Acquire read locks on all vectors BEFORE reading any data
           // // This ensures a consistent view of the entire matrix
           acquireAllVectorReadLocks(ve);
           try {
            int numVectors = this.vectors.length;
            if (numVectors == 0) 
                return new double[0][0];
            if (this.vectors[0].getOrientation() == VectorOrientation.COLUMN_MAJOR) {
                int newRows = this.vectors[0].length(); 
                int newCols = numVectors;
                double[][] result = new double[newRows][newCols];
                for (int i = 0; i < newRows; i++) {
                    for (int j = 0; j < newCols; j++) {  
                        result[i][j] = this.vectors[j].get(i);
                    }
                }
                return result;
            } else {
                double[][] result = new double[numVectors][];
                for (int i = 0; i < numVectors; i++) {
                    result[i] = new double[this.vectors[i].length()];
                    for (int j = 0; j < result[i].length; j++) {
                        result[i][j] = this.vectors[i].get(j);
                    }
                }
                return result;
            }
        } finally {
            releaseAllVectorReadLocks(ve);
        }
    }

    public SharedVector get(int index) { 
        return this.vectors[index]; 
    }

    public int length() {
        int rev=this.vectors.length;
        return rev;
        
    }

    public VectorOrientation getOrientation() {
        if (this.length()==0) {
            throw new IllegalArgumentException();
        }
        VectorOrientation rev=this.vectors[0].getOrientation();
        return rev  ;
    }

    /**
     * Acquires read locks on all vectors in the array.
     * Must be called before reading from multiple vectors to ensure consistency.
     */

    private void acquireAllVectorReadLocks(SharedVector[] vecs) {
        for (int i = 0; i < vecs.length; i++) {
            vecs[i].readLock();
        }
    
    }

    /**
     * Releases read locks on all vectors in the array.
     * Must be called after reading is complete.
     */

    private void releaseAllVectorReadLocks(SharedVector[] vecs) {
        for (int i = 0; i < vecs.length; i++) {
            vecs[i].readUnlock();
        }
    }

    /**
     * Acquires write locks on all vectors in the array.
     */

    private void acquireAllVectorWriteLocks(SharedVector[] vecs) {
        
        for (int i = 0; i < vecs.length; i++) {
            vecs[i].writeLock();
        }
    
    }

    /**
     * Releases write locks on all vectors in the array.
     */
    
    private void releaseAllVectorWriteLocks(SharedVector[] vecs) {
        for (int i = 0; i < vecs.length; i++) {
            vecs[i].writeUnlock();
        }
    }
}