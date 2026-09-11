-- Pig-2  Top 10 most reviewed games
games = LOAD '/steam/games.csv' USING PigStorage(',') AS
        (app_id:int, title:chararray, date_release:chararray, win:chararray, mac:chararray,
         linux:chararray, rating:chararray, positive_ratio:int, user_reviews:int,
         price_final:double, price_original:double, discount:double, steam_deck:chararray);
valid_games = FILTER games BY app_id IS NOT NULL;   -- drops the header row

grp_all = GROUP valid_games ALL;
top10 = FOREACH grp_all {
    srt = ORDER valid_games BY user_reviews DESC;
    lim = LIMIT srt 10;
    GENERATE FLATTEN(lim);
};
final_top10_reviewed = FOREACH top10 GENERATE app_id, title, rating, positive_ratio, user_reviews;
STORE final_top10_reviewed INTO '/steam/pig/Top10Reviewed' USING PigStorage('\t');
