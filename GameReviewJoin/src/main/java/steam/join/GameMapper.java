package steam.join;

import java.io.IOException;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

/**
 * Left side of the join: games.csv.
 * Emits (app_id, "G<TAB>title<TAB>rating") so the reducer can tell a game
 * record apart from review records arriving under the same key.
 */
public class GameMapper extends Mapper<LongWritable, Text, Text, Text> {

    private final Text appId = new Text();
    private final Text tagged = new Text();

    @Override
    protected void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {
        String[] f = value.toString().split(",");
        if (f.length != 13 || f[0].equals("app_id")) {
            return; // header or malformed row
        }
        appId.set(f[0]);
        tagged.set("G\t" + f[1] + "\t" + f[6]);
        context.write(appId, tagged);
    }
}
