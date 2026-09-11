-- Pig-1  Top 5 rating groups by number of games
games = LOAD '/steam/games.csv' USING PigStorage(',') AS
        (app_id:int, title:chararray, date_release:chararray, win:chararray, mac:chararray,
         linux:chararray, rating:chararray, positive_ratio:int, user_reviews:int,
         price_final:double, price_original:double, discount:double, steam_deck:chararray);
valid_games = FILTER games BY app_id IS NOT NULL;   -- drops the header row

grp_for_ratings = GROUP valid_games BY rating;
cnt_for_ratings = FOREACH grp_for_ratings GENERATE group AS rating, COUNT(valid_games) AS num_games;

grp_all = GROUP cnt_for_ratings ALL;
top5_for_ratings = FOREACH grp_all {
    srt = ORDER cnt_for_ratings BY num_games DESC;
    lim = LIMIT srt 5;
    GENERATE FLATTEN(lim);
};
STORE top5_for_ratings INTO '/steam/pig/Top5Ratings' USING PigStorage('\t');
