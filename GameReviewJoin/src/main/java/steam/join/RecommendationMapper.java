package steam.join;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

/**
 * Right side of the join: recommendations.csv (41 million rows).
 *
 * Emitting one record per review would push 41 million records through the
 * shuffle. Instead the mapper keeps a running total per app_id in memory
 * (in-mapper combining) and emits one "R<TAB>reviews<TAB>recommended<TAB>hours"
 * record per game when its input split is finished.
 */
public class RecommendationMapper extends Mapper<LongWritable, Text, Text, Text> {

    // app_id -> {reviews, recommended, hours}
    private final Map<String, double[]> totals = new HashMap<>();

    @Override
    protected void map(LongWritable key, Text value, Context context) {
        // app_id,helpful,funny,date,is_recommended,hours,user_id,review_id
        String[] f = value.toString().split(",");
        if (f.length != 8 || f[0].equals("app_id")) {
            return; // header or malformed row
        }
        try {
            double hours = Double.parseDouble(f[5]);
            double[] t = totals.computeIfAbsent(f[0], k -> new double[3]);
            t[0] += 1;
            t[1] += Boolean.parseBoolean(f[4]) ? 1 : 0;
            t[2] += hours;
        } catch (NumberFormatException e) {
            context.getCounter("Join", "Malformed review rows").increment(1);
        }
    }

    @Override
    protected void cleanup(Context context) throws IOException, InterruptedException {
        Text appId = new Text();
        Text tagged = new Text();
        for (Map.Entry<String, double[]> e : totals.entrySet()) {
            double[] t = e.getValue();
            appId.set(e.getKey());
            tagged.set("R\t" + (long) t[0] + "\t" + (long) t[1] + "\t" + t[2]);
            context.write(appId, tagged);
        }
    }
}
