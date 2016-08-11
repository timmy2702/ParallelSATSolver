package tim.parallel;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.status.StatusLogger;
import tim.Timer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


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
        ((LoggerContext) LogManager.getContext()).getConfiguration().getLoggerConfig(logger.getName()).setLevel(level);
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
        printBuckets();
    }


    /* Private Methods */
    /**
     * This method will parse through the file and initialize the buckets
     */
    private void initBuckets() throws IOException {
        BufferedReader buffer = new BufferedReader(new FileReader(file));

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
                    int variables = Integer.parseInt(split[2]);
                    int clauses = Integer.parseInt(split[3]);
                    logger.warn(String.format("Variables = %d, Clauses = %d", variables, clauses));

                    // init the buckets
                    buckets = new Bucket[variables];
                    for (int i = 0; i < buckets.length; i++) {
                        buckets[i] = new Bucket();
                    }
                }
                else {
                    // create a new clause (assume 0 at the end)
                    int[] clause = new int[split.length - 1];
                    int key = 0;
                    int literal;
                    boolean isPosKey = false;
                    for (int i = 0; i < clause.length; i++) {
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

                    // add the clause into the right bucket
                    buckets[key - 1].add(clause, isPosKey ? Clauses.ClauseType.POSITIVE : Clauses.ClauseType.NEGATIVE);
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
