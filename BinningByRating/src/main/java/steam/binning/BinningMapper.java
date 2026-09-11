package steam.binning;

import java.io.IOException;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;

/**
 * Reads games.csv and writes each game into the bin (output file) named after
 * its Steam rating label, e.g. "Very Positive" -> VeryPositive-m-00000.
 * A counter per bin records how many games landed in it.
 */
public class BinningMapper extends Mapper<LongWritable, Text, Text, NullWritable> {

    private MultipleOutputs<Text, NullWritable> mos;

    @Override
    protected void setup(Context context) {
        mos = new MultipleOutputs<>(context);
    }

    @Override
    protected void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {
        // app_id,title,date_release,win,mac,linux,rating,positive_ratio,user_reviews,
        // price_final,price_original,discount,steam_deck
        String[] f = value.toString().split(",");
        if (f.length != 13 || f[0].equals("app_id")) {
            return; // header or malformed row
        }
        String rating = f[6].trim();
        String bin = rating.replaceAll("[^A-Za-z]", ""); // file names must be alphanumeric
        mos.write("bins", value, NullWritable.get(), bin);
        context.getCounter("Bins", rating).increment(1);
    }

    @Override
    protected void cleanup(Context context) throws IOException, InterruptedException {
        mos.close();
    }
}
