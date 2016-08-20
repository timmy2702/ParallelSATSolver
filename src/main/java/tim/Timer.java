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
     * @return a string of the time result
     */
    public String result() {
        double result = System.currentTimeMillis() - startTime;
        double seconds = result / 60;
        double minutes = seconds / 60;
        double hours = minutes / 24;
        return String.format("Time for '%s' = %.0f milliseconds = %.3f seconds = %.3f minutes = %.3f hours",
                title, result, seconds, minutes, hours);
    }
}
