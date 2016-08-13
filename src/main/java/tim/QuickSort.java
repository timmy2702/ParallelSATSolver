package tim;


import java.util.Arrays;

/**
 * This class is my implementation for quick sort in-place (only support int[] for now)
 * Note: it will sort the array into this:
 * -1 1 -2 2 2 -3 -3 3 -10 10 10
 */
public class QuickSort {

    /* Public Methods */
    /**
     * This method will sort the data in-place by choosing the pivot point as the center item
     * @param data given the array of integers
     */
    public static void sort(int[] data) {
        // base case
        if ((data == null) || (data.length == 0)) {
            return;
        }

        // sort the whole array
        sort(data, 0, data.length - 1);
    }


    /* Private Methods */
    /**
     * This method will sort the array recursively using quick sort algorithm
     * @param data given the array
     * @param low low index
     * @param high high index
     */
    private static void sort(int[] data, int low, int high) {
        // base case
        if ((low == high) || (low > high)) {
            return;
        }

        // get the middle item
        int midIndex = low + (high - low) / 2;
        int midItem = (data[midIndex] > 0) ? data[midIndex] : -data[midIndex];

        // swap left items (smaller)
        int item;
        int index = low;
        boolean isFullPlace = true;
        while (index < midIndex) {
            // assign item as positive number
            item = (data[index] > 0) ? data[index] : -data[index];

            // only swap if the item is larger or equal the midItem
            if (item > midItem) {
                swapLeft(data, index, midIndex);
                midIndex--;
                continue;
            }
            else if (item == midItem) {
                // negative is before positive
                if (data[index] >= data[midIndex]) {
                    swapLeft(data, index, midIndex);
                    midIndex--;
                    continue;
                }
                else {
                    // put the negative to the right place if there are multiple negatives
                    isFullPlace = true;
                    for (int i = midIndex; i > index; i--) {
                        // only swap if it's not full
                        if (data[index] != data[i]) {
                            swap(data, index, i);
                            isFullPlace = false;
                        }
                    }

                    // exit the loop if it's full
                    if (isFullPlace) {
                        break;
                    }
                    continue;
                }
            }

            // increase the index
            index++;
        }

        // swap right items (larger)
        index = high;
        while ((midIndex < index) && (index >= ((high - low) / 2))) {
            // assign item as positive number
            item = (data[index] > 0) ? data[index] : -data[index];

            // only swap if the item is smaller and equal the midItem
            if (item < midItem) {
                swapRight(data, midIndex, index);
                midIndex++;
                continue;
            }
            else if (item == midItem) {
                // negative is before positive
                if (data[index] >= data[midIndex]) {
                    // put the negative to the right place if there are multiple negatives
                    isFullPlace = true;
                    for (int i = midIndex; i < index; i++) {
                        // only swap if it's not full
                        if (data[index] != data[i]) {
                            swap(data, index, i);
                            isFullPlace = false;
                        }
                    }

                    // exit the loop if it's full
                    if (isFullPlace) {
                        break;
                    }
                    continue;
                }
                else {
                    swapRight(data, midIndex, index);
                    midIndex++;
                    continue;
                }
            }

            // decrease the index
            index--;
        }

        // sort on both sides
        sort(data, low, midIndex - 1);
        sort(data, midIndex + 1, high);
    }


    /**
     * This method will swap the two data in place without using temp variable (XOR style)
     * @param data given the data array
     * @param index1 index for 1st item
     * @param index2 index for 2nd item
     */
    private static void swap(int[] data, int index1, int index2) {
        data[index1] ^= data[index2];
        data[index2] ^= data[index1];
        data[index1] ^= data[index2];
    }


    /**
     * This method will swap the data to the left
     * @param data given the data array
     * @param index1 index for 1st item (small)
     * @param index2 index for 2nd item (big)
     */
    private static void swapLeft(int[] data, int index1, int index2) {
        // handle when there are items between
        if (index2 - index1 > 1) {
            swap(data, index2, index2 - 1);
            swap(data, index1, index2);
        }
        else {
            swap(data, index2, index1);
        }
    }


    /**
     * This method will sway the data to the right
     * @param data given the data array
     * @param index1 index for 1st item (small)
     * @param index2 index for 2nd item (big)
     */
    private static void swapRight(int[] data, int index1, int index2) {
        // handle when there are items between
        if (index2 - index1 > 1) {
            swap(data, index1, index1 + 1);
            swap(data, index2, index1);
        }
        else {
            swap(data, index1, index2);
        }
    }
}
