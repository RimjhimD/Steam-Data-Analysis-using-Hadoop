-- Pig-4  Top 10 games by total hours played by their reviewers
recs = LOAD '/steam/recommendations.csv' USING PigStorage(',') AS
       (app_id:int, helpful:int, funny:int, date:chararray, is_recommended:chararray,
        hours:double, user_id:int, review_id:int);
valid_recs = FILTER recs BY app_id IS NOT NULL;     -- drops the header row

grp_for_games = GROUP valid_recs BY app_id;
played = FOREACH grp_for_games GENERATE group AS app_id,
                                        COUNT(valid_recs) AS reviews,
                                        SUM(valid_recs.hours) AS hours;

games = LOAD '/steam/games.csv' USING PigStorage(',') AS
        (app_id:int, title:chararray, date_release:chararray, win:chararray, mac:chararray,
         linux:chararray, rating:chararray, positive_ratio:int, user_reviews:int,
         price_final:double, price_original:double, discount:double, steam_deck:chararray);
game_info = FOREACH (FILTER games BY app_id IS NOT NULL) GENERATE app_id, title, rating;

-- games.csv is small, so it is copied to every mapper (map-side replicated join)
joined = JOIN played BY app_id, game_info BY app_id USING 'replicated';
result = FOREACH joined GENERATE played::app_id AS app_id, game_info::title AS title,
                                 game_info::rating AS rating, played::reviews AS reviews,
                                 played::hours AS hours;

grp_all = GROUP result ALL;
top10 = FOREACH grp_all {
    srt = ORDER result BY hours DESC;
    lim = LIMIT srt 10;
    GENERATE FLATTEN(lim);
};
-- hours stay numeric for the sort and are formatted only for output
final_top10_played = FOREACH top10 GENERATE app_id, title, rating, reviews,
                                            SPRINTF('%.1f', hours) AS hours;
STORE final_top10_played INTO '/steam/pig/Top10Played' USING PigStorage('\t');
