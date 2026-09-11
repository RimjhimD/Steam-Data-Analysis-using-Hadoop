package steam.summary;

import java.io.IOException;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

/**
 * Reads recommendations.csv
 * (app_id, helpful, funny, date, is_recommended, hours, user_id, review_id)
 * and emits (app_id, tuple) for every review, with a count of one.
 */
public class ReviewMapper extends Mapper<LongWritable, Text, Text, ReviewStatsTuple> {

    private final Text appId = new Text();
    private final ReviewStatsTuple tuple = new ReviewStatsTuple();

    @Override
    protected void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {
        String[] f = value.toString().split(",");
        if (f.length != 8 || f[0].equals("app_id")) {
            return; // header or malformed row
        }
        try {
            long helpful = Long.parseLong(f[1]);
            boolean recommended = Boolean.parseBoolean(f[4]);
            double hours = Double.parseDouble(f[5]);
            appId.set(f[0]);
            tuple.set(1, recommended ? 1 : 0, hours, hours, helpful);
            context.write(appId, tuple);
        } catch (NumberFormatException e) {
            context.getCounter("Summary", "Malformed rows").increment(1);
        }
    }
}
