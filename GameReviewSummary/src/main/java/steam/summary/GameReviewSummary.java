package steam.summary;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
 * MR-1  Review summary per game  (numerical summarization pattern).
 *
 * For every app_id in recommendations.csv: number of reviews, number
 * recommended, positive %, total / average / maximum hours played and total
 * helpful votes.
 *
 * Usage: hadoop jar GameReviewSummary.jar steam.summary.GameReviewSummary <in> <out>
 */
public class GameReviewSummary {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: GameReviewSummary <input> <output>");
            System.exit(2);
        }
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "Steam review summary per game");
        job.setJarByClass(GameReviewSummary.class);

        job.setMapperClass(ReviewMapper.class);
        job.setCombinerClass(ReviewReducer.class);
        job.setReducerClass(ReviewReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(ReviewStatsTuple.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
