package steam.summary;

import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * Merges every tuple for one app_id into a single summary.
 * Input and output types match, so the driver also registers this class as
 * the combiner: each mapper pre-merges its own tuples before the shuffle.
 */
public class ReviewReducer extends Reducer<Text, ReviewStatsTuple, Text, ReviewStatsTuple> {

    private final ReviewStatsTuple result = new ReviewStatsTuple();

    @Override
    protected void reduce(Text key, Iterable<ReviewStatsTuple> values, Context context)
            throws IOException, InterruptedException {
        result.set(0, 0, 0, 0, 0);
        for (ReviewStatsTuple t : values) {
            result.merge(t);
        }
        context.write(key, result);
    }
}
