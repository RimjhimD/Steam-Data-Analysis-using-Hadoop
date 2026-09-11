package steam.summary;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;

/**
 * Custom Writable that carries the running totals for one game.
 * Every field is additive (count, sum, max), so the same tuple can be merged
 * in the combiner and again in the reducer without losing accuracy.
 */
public class ReviewStatsTuple implements Writable {

    private long reviews;       // number of recommendations
    private long recommended;   // how many of them were thumbs up
    private double totalHours;  // hours played, summed over reviewers
    private double maxHours;    // most hours any single reviewer played
    private long helpful;       // "helpful" votes, summed

    public ReviewStatsTuple() {
    }

    public void set(long reviews, long recommended, double totalHours, double maxHours, long helpful) {
        this.reviews = reviews;
        this.recommended = recommended;
        this.totalHours = totalHours;
        this.maxHours = maxHours;
        this.helpful = helpful;
    }

    /** Folds another partial result into this one. */
    public void merge(ReviewStatsTuple other) {
        reviews += other.reviews;
        recommended += other.recommended;
        totalHours += other.totalHours;
        maxHours = Math.max(maxHours, other.maxHours);
        helpful += other.helpful;
    }

    public void write(DataOutput out) throws IOException {
        out.writeLong(reviews);
        out.writeLong(recommended);
        out.writeDouble(totalHours);
        out.writeDouble(maxHours);
        out.writeLong(helpful);
    }

    public void readFields(DataInput in) throws IOException {
        reviews = in.readLong();
        recommended = in.readLong();
        totalHours = in.readDouble();
        maxHours = in.readDouble();
        helpful = in.readLong();
    }

    /** reviews, recommended, positive %, total hours, average hours, max hours, helpful votes */
    @Override
    public String toString() {
        double positivePct = reviews == 0 ? 0 : 100.0 * recommended / reviews;
        double avgHours = reviews == 0 ? 0 : totalHours / reviews;
        return reviews + "\t" + recommended + "\t" + String.format("%.1f", positivePct) + "\t"
                + String.format("%.1f", totalHours) + "\t" + String.format("%.1f", avgHours) + "\t"
                + String.format("%.1f", maxHours) + "\t" + helpful;
    }
}
