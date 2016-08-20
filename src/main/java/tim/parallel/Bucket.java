package tim.parallel;

import org.apache.logging.log4j.Level;

import java.util.*;


/**
 * This class represents a bucket in Rina's algorithm.
 */
public class Bucket {

    /* Declare Variables */
    private Map<Integer,List<int[]>> clauseCodes;
    private List<Clauses> posClauses;
    private List<Clauses> negClauses;
    private Clauses posCurrent;
    private Clauses negCurrent;
    private int posSize;
    private int negSize;
    private int posClauseMaxSize;
    private int negClauseMaxSize;
    private int key;


    /* Class Constructors */
    public Bucket() {
        clauseCodes = new HashMap<>();
        posClauses = new LinkedList<>();
        negClauses = new LinkedList<>();
        posCurrent = new Clauses();
        negCurrent = new Clauses();
        posSize = 0;
        negSize = 0;
        posClauseMaxSize = 0;
        negClauseMaxSize = 0;
        key = 0;

        // add current to the list clauses
        posClauses.add(posCurrent);
        negClauses.add(negCurrent);
    }


    /* Class Operators */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        // add the size variables
        result.append("posSize = ");
        result.append(posSize);
        result.append(", negSize = ");
        result.append(negSize);
        result.append(", posClauseMaxSize = ");
        result.append(posClauseMaxSize);
        result.append(", negClauseMaxSize = ");
        result.append(negClauseMaxSize);
        result.append("\n");

        if (Solver.debugLevel == Level.INFO) {
            // loop and add posClauses
            for (int i = 0; i < posClauses.size(); i++) {
                result.append("posClauses ");
                result.append(i + 1);
                result.append("\n");
                result.append(posClauses.get(i));
                result.append("\n");
            }

            // loop and add negClauses
            for (int i = 0; i < negClauses.size(); i++) {
                result.append("negClauses ");
                result.append(i + 1);
                result.append("\n");
                result.append(negClauses.get(i));
                result.append("\n");
            }
        }

        return result.toString();
    }


    /* Public Methods */
    /**
     * This method will add a clause into the bucket
     * @param clause given an array of literals for this clause
     */
    public void add(int[] clause, Clauses.ClauseType type) {
        // don't add null clause
        assert (clause != null) : "Adding null clause";

        // don't handle if clause existed in the bucket
        if (isClauseExisted(clause, true)) {
            return;
        }

        // add the clause into the bucket
        switch (type) {
            case POSITIVE:
                // handle when add the clause to current clauses unsuccessfully
                if (!posCurrent.add(clause)) {
                    posCurrent = new Clauses();
                    posCurrent.add(clause);
                    posClauses.add(posCurrent);
                }

                // increase the size
                posSize++;

                // get the max size
                posClauseMaxSize = (posClauseMaxSize < clause.length) ? clause.length : posClauseMaxSize;

                // get key (assume clause is sorted)
                key = clause[0];
                break;

            case NEGATIVE:
                // handle when add the clause to current clauses unsuccessfully
                if (!negCurrent.add(clause)) {
                    negCurrent = new Clauses();
                    negCurrent.add(clause);
                    negClauses.add(negCurrent);
                }

                // increase the size
                negSize++;

                // get the max size
                negClauseMaxSize = (negClauseMaxSize < clause.length) ? clause.length : negClauseMaxSize;

                // get key (assume clause is sorted)
                key = -clause[0];
                break;
        }
    }


    /**
     * This method will union the given bucket with this bucket
      * @param bucket given the bucket
     */
    public synchronized void union(Bucket bucket) {
        // get the total size (slow code)
        int total = 0;
        if (Solver.debugLevel == Level.INFO) {
            total = posSize + negSize + bucket.getPosSize() + bucket.getNegSize() - getAmountOfDuplicates(this, bucket);
        }

        // get clause max sizes
        posClauseMaxSize = (posClauseMaxSize < bucket.getPosClauseMaxSize()) ?
                bucket.getPosClauseMaxSize() : posClauseMaxSize;
        negClauseMaxSize = (negClauseMaxSize < bucket.getNegClauseMaxSize()) ?
                bucket.getNegClauseMaxSize() : negClauseMaxSize;

        // fill out the empty items for posCurrent
        boolean isAddedAllowed = true;
        boolean isBucketEmpty = false;
        int[] lastItem = null;
        try {
            do {
                lastItem = bucket.pop(Clauses.ClauseType.POSITIVE);

                // only add if it doesn't exist duplicates
                if (!isClauseExisted(lastItem)) {
                    isAddedAllowed = posCurrent.add(lastItem);

                    // increase the size
                    if (isAddedAllowed) {
                        posSize++;
                    }
                }
            } while (isAddedAllowed);

        }
        catch (IndexOutOfBoundsException e) {
            isBucketEmpty = true;
        }

        if (!isBucketEmpty) {
            // add the posClauses and update posCurrent
            posClauses.addAll(bucket.getPosClauses());
            posSize += bucket.getPosSize();
            posCurrent = bucket.getPosCurrent();

            // add the last item back because the posCurrent is full
            this.add(lastItem, Clauses.ClauseType.POSITIVE);
        }

        // fill out the empty items for negCurrent
        isAddedAllowed = true;
        isBucketEmpty = false;
        try {
            do {
                lastItem = bucket.pop(Clauses.ClauseType.NEGATIVE);
                // only add if it doesn't exist duplicates
                if (!isClauseExisted(lastItem)) {
                    isAddedAllowed = negCurrent.add(lastItem);

                    // increase the size
                    if (isAddedAllowed) {
                        negSize++;
                    }
                }
            } while (isAddedAllowed);
        }
        catch (IndexOutOfBoundsException e) {
            isBucketEmpty = true;
        }

        if (!isBucketEmpty) {
            // add the negClauses and update negCurrent
            negClauses.addAll(bucket.getNegClauses());
            negSize += bucket.getNegSize();
            negCurrent = bucket.getNegCurrent();
            // add the last item back
            this.add(lastItem, Clauses.ClauseType.NEGATIVE);
        }

        // check the total
        if (Solver.debugLevel == Level.INFO) {
            assert (total == (posSize + negSize)) :
                    String.format("Total size after union is wrong -- expected %d -- actual %d\n%s", total, posSize + negSize, this);
        }
    }


    /**
     * This method will pop the last item of the bucket clause type
     * @return the last item
     */
    public int[] pop(Clauses.ClauseType type) {
        int[] result = null;
        switch (type) {
            case POSITIVE:
                // don't handle when there is nothing
                if (posSize == 0) {
                    throw new IndexOutOfBoundsException();
                }

                // try to get the last item of the current clause
                try {
                    result = posCurrent.pop();
                }
                catch (IndexOutOfBoundsException e) {
                    // remove the last clause in the list
                    posClauses.remove(posClauses.size() - 1);
                    posCurrent = posClauses.get(posClauses.size() - 1);
                    result = posCurrent.pop();
                }

                // reduce the size
                posSize--;
                break;

            case NEGATIVE:
                // don't handle when there is nothing
                if (negSize == 0) {
                    throw new IndexOutOfBoundsException();
                }

                // try to get the last item of the current clause
                try {
                    result = negCurrent.pop();
                }
                catch (IndexOutOfBoundsException e) {
                    // remove the last clause in the list
                    negClauses.remove(negClauses.size() - 1);
                    negCurrent = negClauses.get(negClauses.size() - 1);
                    result = negCurrent.pop();
                }

                // reduce the size
                negSize--;
                break;
        }

        return result;
    }


    /**
     * This method will return an iterator that loops up to a specific size
     * @param size given the size that you want to cut off
     * @param type given the type of the clause
     * @return an iterator for each clause
     */
    public Iterator<int[]> getIterator(final int size, final Clauses.ClauseType type) {
        // don't handle when size is greater than current negSize or posSize
        if (((type == Clauses.ClauseType.POSITIVE) && (size > posSize)) ||
                ((type == Clauses.ClauseType.NEGATIVE) && (size > negSize))) {
            return null;
        }

        // return a custom iterator
        return new Iterator<int[]>() {

            /* Declaring Variables */
            private int index = 0;
            private int clausesIndex = -1;
            private Clauses current;


            /* Class Operators */
            @Override
            public boolean hasNext() {
                // check whether current index exceeds the size
                return index < size;
            }


            @Override
            public int[] next() {
                // calculate the new clauses index
                int newIndex = index / Clauses.SIZE_THRESHOLD;

                // get the next clause in clauses
                switch (type) {
                    case POSITIVE:
                        // handle the new index
                        if (newIndex != clausesIndex) {
                            current = posClauses.get(newIndex);
                        }
                        break;

                    case NEGATIVE:
                        // handle the new index
                        if (newIndex != clausesIndex) {
                            current = negClauses.get(newIndex);
                        }
                        break;
                }
                int[] result = current.get(index % Clauses.SIZE_THRESHOLD);
                clausesIndex = newIndex;

                // increase the index and return
                index++;
                return result;
            }


            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }


    /**
     * This method will get the amount of duplicates between two buckets (mainly for debugging)
     * @param bucket1 given 1st bucket
     * @param bucket2 given 2nd bucket
     * @return number of duplicates
     */
    public static int getAmountOfDuplicates(Bucket bucket1, Bucket bucket2) {
        // init variables
        int result = 0;
        int[] clause;
        Iterator<int[]> posIterator = bucket2.getIterator(bucket2.getPosSize(), Clauses.ClauseType.POSITIVE);
        Iterator<int[]> negIterator = bucket2.getIterator(bucket2.getNegSize(), Clauses.ClauseType.NEGATIVE);

        // calculate the number of duplicates
        while (posIterator.hasNext()) {
            clause = posIterator.next();
            if (bucket1.isClauseExisted(clause)) {
                System.out.format("Duplicate = %s\n\n", Arrays.toString(clause));
                result++;
            }
        }

        while (negIterator.hasNext()) {
            clause = negIterator.next();
            if (bucket1.isClauseExisted(clause)) {
                System.out.format("Duplicate = %s\n\n", Arrays.toString(clause));
                result++;
            }
        }

        return result;
    }


    /**
     * This method will check whether this clause is existed or not (without adding)
     * @param clause given the clause
     * @return check whether it is existed
     */
    public boolean isClauseExisted(int[] clause) {
        return isClauseExisted(clause, false);
    }


    /* Private Methods */
    /**
     * This method will find whether the given clause is already existed in the bucket or not
     * @param clause given the clause
     * @param isAddingCode whether code should be added to the system or not
     * @return is duplicate exists
     */
    private boolean isClauseExisted(int[] clause, boolean isAddingCode) {
        // create a hash code for this clause
        int code = 0;
        for (int literal : clause) {
            code += literal * 3 + (literal - 1) * 5 + 1;
        }

        // put the code to clauseCodes and check for duplicates
        List<int[]> codes = clauseCodes.get(code);
        if (codes == null) {
            // add the code to the system
            if (isAddingCode) {
                codes = new ArrayList<>();
                codes.add(clause);
                clauseCodes.put(code,codes);
            }
        }
        else {
            // check for duplicates
            int i;
            boolean isBreak = false;
            for (int[] item : codes) {
                // compare item and clause assuming in sorted order
                if (item.length == clause.length) {
                    isBreak = false;
                    for (i = 0; i < clause.length; i++) {
                        if (clause[i] != item[i]) {
                            isBreak = true;
                            break;
                        }
                    }

                    // break out of the loop if found duplicates
                    if (!isBreak) {
                        break;
                    }
                }
                else {
                    isBreak = true;
                }
            }

            // handle duplicates found
            if (!isBreak) {
                return true;
            }
            else {
                // add the code to the system
                if (isAddingCode) {
                    codes.add(clause);
                }
            }
        }

        return false;
    }


    /**
     * This method is used for debugging/printing the clauseCodes object
     */
    private void printClauseCodes() {
        System.out.println("Printing clauseCodes");
        for (Map.Entry<Integer,List<int[]>> entry : clauseCodes.entrySet()) {
            System.out.format("Code = %d\n", entry.getKey());
            for (int[] clause : entry.getValue()) {
                System.out.println(Arrays.toString(clause));
            }
        }
    }


    /* Getters & Setters */
    public List<Clauses> getPosClauses() {
        return posClauses;
    }


    public List<Clauses> getNegClauses() {
        return negClauses;
    }


    public Clauses getPosCurrent() {
        return posCurrent;
    }


    public Clauses getNegCurrent() {
        return negCurrent;
    }


    public int getPosSize() {
        return posSize;
    }


    public int getNegSize() {
        return negSize;
    }


    public int getPosClauseMaxSize() {
        return posClauseMaxSize;
    }


    public int getNegClauseMaxSize() {
        return negClauseMaxSize;
    }


    public int getKey() {
    	assert (key >= 0) : String.format("key must be positive, but key = %d", key);
        return key;
    }
}
