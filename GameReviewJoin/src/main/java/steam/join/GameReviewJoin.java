package steam.join;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
 * MR-3  Games joined with their reviews  (join pattern: reduce-side join).
 *
 * Two inputs, each with its own mapper (MultipleInputs), meet in the reducer
 * on app_id. The result puts each game's title and rating next to how many
 * people reviewed it and how many hours they played.
 *
 * Usage: hadoop jar GameReviewJoin.jar steam.join.GameReviewJoin <games> <recommendations> <out>
 */
public class GameReviewJoin {

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage: GameReviewJoin <games> <recommendations> <output>");
            System.exit(2);
        }
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "Steam games joined with reviews");
        job.setJarByClass(GameReviewJoin.class);

        MultipleInputs.addInputPath(job, new Path(args[0]), TextInputFormat.class, GameMapper.class);
        MultipleInputs.addInputPath(job, new Path(args[1]), TextInputFormat.class, RecommendationMapper.class);
        job.setReducerClass(JoinReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        FileOutputFormat.setOutputPath(job, new Path(args[2]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
