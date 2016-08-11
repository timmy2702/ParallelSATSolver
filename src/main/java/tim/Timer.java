package tim;

/**
 * This class is used to calculate the time it takes for a process
 */
public class Timer {

    /* Declaring Variables */
    private long startTime;
    private String title;


    /* Class Constructors */
    public Timer(String title) {
        this.title = title;
        startTime = System.currentTimeMillis();
    }


    /* Public Methods */
    /**
     * This method will print out the time elapsed
     */
    public void result() {
        System.out.format("Time for '%s' = %d milliseconds\n", title, System.currentTimeMillis() - startTime);
    }
}
