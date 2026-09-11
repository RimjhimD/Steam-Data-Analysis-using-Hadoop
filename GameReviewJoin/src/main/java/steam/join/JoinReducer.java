package steam.join;

import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * Inner join on app_id. For each game the reducer receives one "G" record
 * (title, rating) and several "R" records (partial review totals from each
 * mapper). It adds up the R records and writes one joined line:
 *
 *   app_id  title  rating  reviews  positive%  total_hours  avg_hours
 *
 * Games with no reviews, and reviews whose game is missing from games.csv,
 * are dropped and counted.
 */
public class JoinReducer extends Reducer<Text, Text, Text, Text> {

    private final Text out = new Text();

    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {
        String title = null;
        String rating = null;
        long reviews = 0;
        long recommended = 0;
        double hours = 0;

        for (Text v : values) {
            String[] f = v.toString().split("\t");
            if (f[0].equals("G")) {
                title = f[1];
                rating = f[2];
            } else {
                reviews += Long.parseLong(f[1]);
                recommended += Long.parseLong(f[2]);
                hours += Double.parseDouble(f[3]);
            }
        }

        if (title == null) {
            context.getCounter("Join", "Reviews with no matching game").increment(reviews);
            return;
        }
        if (reviews == 0) {
            context.getCounter("Join", "Games with no reviews").increment(1);
            return;
        }
        context.getCounter("Join", "Joined games").increment(1);
        out.set(title + "\t" + rating + "\t" + reviews + "\t"
                + String.format("%.1f", 100.0 * recommended / reviews) + "\t"
                + String.format("%.1f", hours) + "\t"
                + String.format("%.1f", hours / reviews));
        context.write(key, out);
    }
}
