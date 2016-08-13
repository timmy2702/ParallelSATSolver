package tim.parallel;

import java.util.*;
import java.util.concurrent.Callable;


/**
 * This class will handle the resolution and redistribution task for each thread/worker.
 */
public class WorkerTask implements Callable<Boolean> {

    /* Declare Variables */
    public static final Random random = new Random();

    private Map<Integer,Bucket> data;
    private Bucket[] buckets;
    private Iterator<int[]> posData;
    private int[][] negData;
    private int maxResolutionSize;


    /* Class Constructors */
    public WorkerTask(int maxResolutionSize, int[][] negData, Iterator<int[]> posData, Bucket[] buckets) {
        this.maxResolutionSize = maxResolutionSize;
        this.negData = negData;
        this.posData = posData;
        this.buckets = buckets;
        data = new HashMap<>();
    }


    /* Class Operators */
    @Override
    public Boolean call() {
        // init necessary variables
        int[] resolutionTmp = new int[maxResolutionSize];
        int[] posClause, negClause, resolution;
        int i, j, posIndex, negIndex, posItem, negItem, resolutionLength;
        boolean isTrueClause;

        // do resolution on this bucket
        while (posData.hasNext()) {
            // get posClause
            posClause = posData.next();

            // loop through the negData for negClause
            for (i = 0; i < negData.length; i++) {
                // get negClause
                negClause = negData[i];

                // handle special clause (bad resolution)
                if ((posClause.length == 1) && (negClause.length == 1)) {
                    return true;
                }

                // handle posClause length 1
                if (posClause.length == 1) {
                    resolution = new int[negClause.length - 1];
                    for (j = 0; j < resolution.length; j++) {
                        resolution[j] = negClause[j+1];
                    }

                    // add resolution to bucket
                    addToBucket(resolution);
                }
                // handle negClause length 1
                else if (negClause.length == 1) {
                    resolution = new int[posClause.length - 1];
                    for (j = 0; j < resolution.length; j++) {
                        resolution[j] = posClause[j+1];
                    }

                    // add resolution to bucket
                    addToBucket(resolution);
                }
                // handle when both negClause and posClause > 1
                else {
                    // loop over posClause and negClause for resolutionLength (ignore the first item)
                    resolutionLength = 0;
                    posIndex = 1;
                    negIndex = 1;
                    isTrueClause = false;
                    // debug
                    System.out.format("posClause = %s -- negClause = %s\n",
                            Arrays.toString(posClause), Arrays.toString(negClause));
                    while ((posIndex < posClause.length) || (negIndex < negClause.length)) {
                        // debug
                        System.out.format("resolutionTmp = %s -- posIndex = %d -- negIndex = %d\n",
                                Arrays.toString(resolutionTmp), posIndex, negIndex);

                        // handle true clauses
                        if (posClause[posIndex] == -negClause[negIndex]) {
                            isTrueClause = true;
                            break;
                        }

                        // convert items to positive
                        posItem = (posClause[posIndex] < 0) ? -posClause[posIndex] : posClause[posIndex];
                        negItem = (negClause[negIndex] < 0) ? -negClause[negIndex] : negClause[negIndex];

                        // add small items first
                        if (posItem <= negItem) {
                            resolutionTmp[resolutionLength++] = posClause[posIndex++];
                        }
                        else if (posItem > negItem) {
                            resolutionTmp[resolutionLength++] = negClause[negIndex++];
                        }
                    }

                    // don't add true clauses
                    if (!isTrueClause) {
                        // create resolution
                        resolution = new int[resolutionLength];
                        for (j = 0; j < resolutionLength; j++) {
                            resolution[j] = resolutionTmp[j];
                        }

                        // add resolution to bucket
                        addToBucket(resolution);
                    }
                }
            }
        }

        // do distribution to the right bucket randomly
        redistributeData();

        return false;
    }


    /* Private Methods */
    /**
     * This method will add the clause into the right bucket in data
     * @param clause given the clause (must be sorted)
     */
    private void addToBucket(int[] clause) {
        // debug
        System.out.format("Add to bucket = %s", Arrays.toString(clause));

        int key = (clause[0] < 0) ? -clause[0] : clause[0];

        // handle when key doesn't exist
        if (!data.containsKey(key)) {
            Bucket bucket = new Bucket();
            bucket.add(clause, (key < 0) ? Clauses.ClauseType.NEGATIVE : Clauses.ClauseType.POSITIVE);
            data.put(key, bucket);
        }
        else {
            data.get(key).add(clause, (key < 0) ? Clauses.ClauseType.NEGATIVE : Clauses.ClauseType.POSITIVE);
        }
    }


    /**
     * This method will redistribute the data to the right bucket using 'union'
     */
    private void redistributeData() {
        // random the values in data
        List<Object> values = Arrays.asList((Object[]) data.values().toArray());
        Collections.shuffle(values);

        // loop through values and union to the right bucket
        Bucket bucket;
        for (Object obj : values) {
            bucket = (Bucket) obj;
            buckets[bucket.getKey() - 1].union(bucket);
        }
    }
}
