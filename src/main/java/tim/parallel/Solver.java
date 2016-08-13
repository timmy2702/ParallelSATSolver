package tim.parallel;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import tim.QuickSort;
import tim.Timer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.*;


/**
 * This class is a parallel SAT Solver based on Rina's paper
 */
public class Solver {

    /* Declare Variables */
    private String file;
    private Logger logger;
    private Bucket[] buckets;


    /* Class Constructors */
    public Solver(String file, Level level) {
        this.file = file;

        // set level for the logger & turn off status logger warnings
        logger = LogManager.getLogger(Solver.class.getName());
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(logger.getName());
        loggerConfig.setLevel(level);
        context.updateLoggers();
     }


    /* Public Methods */
    /**
     * This method will run the solver
     */
    public void run() throws Exception {
        // read the file and init buckets
        logger.warn("Init Buckets");
        Timer timerInitBuckets = new Timer("Init Buckets");
        initBuckets();
        timerInitBuckets.result();

        // create a thread pool
        int cores = Runtime.getRuntime().availableProcessors();
        logger.warn(String.format("Available Cores = %d", cores));
        ExecutorService threadPool = Executors.newFixedThreadPool(cores);
        CompletionService<Boolean> completionService = new ExecutorCompletionService<>(threadPool);

        // print original buckets
        printBuckets();

        // loop through each bucket and do resolution
        Bucket bucket;
        WorkerTask worker;
        Iterator<int[]> iterator;
        Future<Boolean> result;
        Boolean isUnsatisfiable;
        int[][][] negData = new int[cores][][];
        int i, j, negCutOff, negCount, negMod, maxResolutionSize, receivedResults;
        for (i = 0; i < buckets.length; i++) {
            bucket = buckets[i];

            // print the bucket
            logger.warn(String.format("Starting Iteration %d", i + 1));
            logger.info(String.format("Bucket %d\n%s", i + 1, bucket));

            // get iterator and necessary data
            iterator = bucket.getIterator(bucket.getNegSize(), Clauses.ClauseType.NEGATIVE);
            negCutOff = bucket.getNegSize() / cores;
            negMod = bucket.getNegSize() % cores;
            negCount = 0;
            maxResolutionSize = bucket.getPosClauseMaxSize() + bucket.getNegClauseMaxSize();

            // init the negData for this bucket
            for (j = 0; j < negData.length; j++) {
                negData[j] = (j < negMod) ? (new int[negCutOff + 1][]) : (new int[negCutOff][]);
            }

            // loop through the negIterator to assign negData
            while (iterator.hasNext()) {
                // assign one each horizontally
                negData[negCount % cores][negCount / cores] = iterator.next();
                negCount++;
            }

            // print out negData
            logger.info(String.format("Printing negData -- negCutOff = %d -- negMod = %d", negCutOff, negMod));
            printArray(negData);

            // submit tasks to thread pool
            logger.warn(String.format("Submitting tasks Iteration %d", i + 1));
            for (j = 0; j < cores; j++) {
                iterator = bucket.getIterator(bucket.getPosSize(), Clauses.ClauseType.POSITIVE);
                worker = new WorkerTask(maxResolutionSize, negData[j], iterator, buckets);
                completionService.submit(worker);
            }

            // getting the results
            logger.warn(String.format("Waiting for results Iteration %d", i + 1));
            receivedResults = 0;
            while (receivedResults < cores) {
                // wait until a result is collected
                result = completionService.take();

                // get the result
                isUnsatisfiable = result.get();
                if (isUnsatisfiable) {
                    logger.error("UNSATISFIABLE");
                    threadPool.shutdownNow();
                    return;
                }
                else {
                    receivedResults++;
                    logger.info(String.format("receivedResults = %d", receivedResults));
                }
            }
        }

        // return result in the end and shutdown
        logger.error("SATISFIABLE");
        threadPool.shutdownNow();
    }


    /* Private Methods */
    /**
     * This method will parse through the file and initialize the buckets
     */
    private void initBuckets() throws IOException {
        // init reusable variables
        BufferedReader buffer = new BufferedReader(new FileReader(file));
        int i, variables, clauses, key, literal, duplicateCount, clauseIndex;
        boolean isPosKey, isTrueClause;
        int[] clause, tmp;

        // read each line in the file
        String line = buffer.readLine();
        while (line != null) {
            // ignore all the lines starting with c
            if (!line.startsWith("c")) {
                // split the line
                String[] split = line.split(" ");

                // handle line with 'p' (assume all the files have 'p' for problem)
                if (split[0].equals("p")) {
                    // get variables and clauses
                    variables = Integer.parseInt(split[2]);
                    clauses = Integer.parseInt(split[3]);
                    logger.warn(String.format("Variables = %d, Clauses = %d", variables, clauses));

                    // init the buckets
                    buckets = new Bucket[variables];
                    for (i = 0; i < buckets.length; i++) {
                        buckets[i] = new Bucket();
                    }
                }
                else {
                    // create a new clause (assume 0 at the end)
                    clause = new int[split.length - 1];
                    key = 0;
                    isPosKey = false;
                    for (i = 0; i < clause.length; i++) {
                        literal = Integer.parseInt(split[i]);
                        clause[i] = literal;

                        // init key for the first one
                        if (i == 0) {
                            key = (literal < 0) ? -literal : literal;
                            isPosKey = literal > 0;
                        }
                        else {
                            // handle the key (won't have 0)
                            if ((literal < 0) && (key > -literal)) {
                                key = -literal;
                                isPosKey = false;
                            }
                            else if ((literal > 0) && (key > literal)) {
                                key = literal;
                                isPosKey = true;
                            }
                        }
                    }

                    // sort the clause
                    QuickSort.sort(clause);
                   assert (Clauses.inOrder(clause));
                    // a simple but correct intsort
                    //QuickSort.intsort(clause);

                    // loop through the sorted clause to check duplicates
                    duplicateCount = 0;
                    isTrueClause = false;
                    for (i = 0; i < (clause.length - 1); i++) {
                        // look ahead one item
                        if (clause[i+1] == clause[i]) {
                            duplicateCount++;
                        }
                        else if (clause[i+1] == -clause[i]) {
                            // don't handle true clauses
                            isTrueClause = true;
                            break;
                        }
                    }

                    // don't add if it's true clause
                    if (!isTrueClause) {
                        // get rid of duplicates
                        if (duplicateCount > 0) {
                            tmp = clause;
                            clause = new int[tmp.length - duplicateCount];
                            clauseIndex = 0;
                            for (i = 0; i < (tmp.length - 1); i++) {
                                clause[clauseIndex] = tmp[i];

                                // look ahead one item
                                if (tmp[i+1] != tmp[i]) {
                                    clause[++clauseIndex] = tmp[i+1];
                                }
                            }
                        }

                        // add the clause into the right bucket
                        buckets[key - 1].add(clause, isPosKey ? Clauses.ClauseType.POSITIVE : Clauses.ClauseType.NEGATIVE);
                    }
                }
            }

            // read the next line
            line = buffer.readLine();
        }
    }


    /**
     * This method will print the buckets out (for debug purposes)
     */
    private void printBuckets() {
        for (int i = 0; i < buckets.length; i++) {
            logger.info(String.format("Bucket %d\n%s", i + 1, buckets[i]));
        }
    }


    /**
     * This method will print out the array (for debug purposes)
     * @param data given the array data
     */
    private void printArray(int[][][] data) {
        int[][] subData;
        for (int i = 0; i < data.length; i++) {
            subData = data[i];
            logger.info(String.format("Array %d -- size = %d", i + 1, subData.length));
            for (int[] item : subData) {
                logger.info(Arrays.toString(item));
            }
        }
    }


    /* Main */
    public static void main(String[] args) throws Exception {
        // Usage: Solver <input-file> <debug-mode (default: INFO)>
        Timer timerTotalProgram = new Timer("Total Time");

        // check args
        if (args.length < 1) {
            System.out.println("Usage: Solver <input-file> <debug-mode (default: INFO)>");
            System.exit(1);
        }

        // handle args
        String file = args[0];
        Level logLevel = Level.INFO;
        if (args.length == 2) {
            switch (args[1].toUpperCase()) {
                case "WARN":
                    logLevel = Level.WARN;
                    break;

                case "ERROR":
                    logLevel = Level.ERROR;
                    break;

                case "FATAL":
                    logLevel = Level.FATAL;
                    break;

                case "OFF":
                    logLevel = Level.OFF;
                    break;

                default:
                    System.out.println("Usage: Solver <input-file> <debug-mode (default: INFO)>");
                    System.exit(1);
            }
        }

        // init and run the solver
        (new Solver(file, logLevel)).run();

        // get the time
        timerTotalProgram.result();
    }
}
