package steam.binning;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.LazyOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

/**
 * MR-2  Binning games by rating  (data organization pattern: binning).
 *
 * Map-only job. Every game is written to one of nine output files, one per
 * Steam rating label, using MultipleOutputs. No reducer and no shuffle.
 *
 * Usage: hadoop jar BinningByRating.jar steam.binning.BinningByRating <in> <out>
 */
public class BinningByRating {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: BinningByRating <input> <output>");
            System.exit(2);
        }
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "Steam binning games by rating");
        job.setJarByClass(BinningByRating.class);

        job.setMapperClass(BinningMapper.class);
        job.setNumReduceTasks(0);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(NullWritable.class);

        MultipleOutputs.addNamedOutput(job, "bins", TextOutputFormat.class, Text.class, NullWritable.class);
        // only create the bin files; skip the empty default part-m-00000
        LazyOutputFormat.setOutputFormatClass(job, TextOutputFormat.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
