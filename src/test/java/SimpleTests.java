import org.junit.Assert;
import org.junit.Test;
import tim.QuickSort;
import tim.parallel.Bucket;
import tim.parallel.Clauses;


/**
 * This class is for simple tests for some functions in this program
 */
public class SimpleTests {

    @Test
    public void testQuickSort() {
        int[] test1 = {1, 2, 3, 4, 5, 6};
        int[] test2 = {5, -5, 7, 6, -6, 1, -1, 2, 3, -1, -3, 4};
        int[] result2 = {-1, -1, 1, 2, -3, 3, 4, -5, 5, -6, 6, 7};
        int[] test3 = {-11, 8, 9};
        int[] result3 = {8, 9, -11};
        QuickSort.sort(test1);
        QuickSort.sort(test2);
        QuickSort.sort(test3);
        Assert.assertArrayEquals(test1, test1);
        Assert.assertArrayEquals(test2, result2);
        Assert.assertArrayEquals(test3, result3);
    }
}
