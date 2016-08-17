package tim.parallel;


import java.util.Arrays;

/**
 * This class represents an array of clauses (clause is represented as int[])
 */
public class Clauses {

    /* Declare Variables */
    public static final int SIZE_THRESHOLD = 100;
    public enum ClauseType {
        POSITIVE,
        NEGATIVE
    }

    private int[][] clauses;
    private int size;


    /* Class Constructors */
    public Clauses() {
        clauses = new int[SIZE_THRESHOLD][];
        size = 0;
    }


    /* Class Operators */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        // add the size variable
        result.append("size = ");
        result.append(size);
        result.append("\n");

        // loop and add the clauses
        for (int i = 0; i < size; i++) {
            result.append(Arrays.toString(clauses[i]));
            result.append("\n");
        }

        return result.toString();
    }


    /* Public Methods */
    /**
     * This method will add a clause to the class.
     * @param clause array of literals for that clause
     * @return false if reaches the threshold. true if successfully added
     */
    public boolean add(int[] clause) {
        // don't add when reaches threshold
        if (size == SIZE_THRESHOLD) {
            return false;
        }

        // add the clause & increase the size
        clauses[size++] = clause;
        return true;
    }


    /**
     * This method will return the clause based on the given index
     * @param clauseIndex given the index
     * @return an array of literals of this clause
     */
    public int[] get(int clauseIndex) {
        return clauses[clauseIndex];
    }


    /**
     * This method will pop the last item out
     * @return the last clause item in this array
     */
    public int[] pop() {
        // don't handle when the clauses is empty
        if (size == 0) {
            throw new IndexOutOfBoundsException();
        }

        return clauses[--size];
    }


    /**
     * Khanh added for testing
     * @param clause given the clause array
     * @return whether this clause is in correct order or not
     */
    public static boolean inOrder(int[] clause) {
    	for (int i = 0; i < clause.length - 2; ++i) {
    		if (Math.abs(clause[i]) > Math.abs(clause[i+1]))
    			return false;
    	}
    	return true;
    }
}
