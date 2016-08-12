package tim.parallel;

import java.util.*;
import java.util.concurrent.Callable;


/**
 * This class will handle the resolution and redistribution task for each thread/worker.
 */
public class WorkerTask implements Callable<Boolean> {

    /* Declare Variables */
    public static final Random random = new Random();

    private Bucket[] buckets;
    private Iterator<int[]> posData;
    private int[][] negData;
    private int id;


    /* Class Constructors */
    public WorkerTask(int id, int[][] negData, Iterator<int[]> posData, Bucket[] buckets) {
        this.id = id;
        this.negData = negData;
        this.posData = posData;
        this.buckets = buckets;
    }


    /* Class Operators */
    @Override
    public Boolean call() {
        // init necessary variables
        Map<Integer,Bucket> data = new HashMap<>();

        // do resolution on this bucket
        int[] posClause;
        int[] negClause;
        int key;
        while (posData.hasNext()) {
            // get posClause and sort the array
            posClause = posData.next();

            // loop through the negData for negClause
            for (int i = 0; i < negData.length; i++) {
                negClause = negData[i];

                // handle special clause (bad resolution)
                if ((posClause.length == 1) && (negClause.length == 1)) {
                    return true;
                }

                // do resolution over 2 clauses

            }
        }

        return false;
    }
}
