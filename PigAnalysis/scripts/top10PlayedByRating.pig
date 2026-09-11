-- Pig-5  Top 10 games by total hours played inside each rating group
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

joined = JOIN played BY app_id, game_info BY app_id USING 'replicated';
result = FOREACH joined GENERATE game_info::rating AS rating, played::app_id AS app_id,
                                 game_info::title AS title, played::reviews AS reviews,
                                 played::hours AS hours;

grp_for_ratings = GROUP result BY rating;
top10_played_ratings = FOREACH grp_for_ratings {
    sorted = ORDER result BY hours DESC;
    top10 = LIMIT sorted 10;
    GENERATE FLATTEN(top10);
};
-- hours stay numeric for the sort and are formatted only for output
final_top10_played_by_rating = FOREACH top10_played_ratings GENERATE rating, app_id, title, reviews,
                                                                     SPRINTF('%.1f', hours) AS hours;
STORE final_top10_played_by_rating INTO '/steam/pig/Top10PlayedByRating' USING PigStorage('\t');
