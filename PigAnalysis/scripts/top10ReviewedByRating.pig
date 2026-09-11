-- Pig-3  Top 10 most reviewed games inside each rating group
games = LOAD '/steam/games.csv' USING PigStorage(',') AS
        (app_id:int, title:chararray, date_release:chararray, win:chararray, mac:chararray,
         linux:chararray, rating:chararray, positive_ratio:int, user_reviews:int,
         price_final:double, price_original:double, discount:double, steam_deck:chararray);
valid_games = FILTER games BY app_id IS NOT NULL;   -- drops the header row

grp_for_ratings = GROUP valid_games BY rating;
top10_reviewed_ratings = FOREACH grp_for_ratings {
    sorted = ORDER valid_games BY user_reviews DESC;
    top10 = LIMIT sorted 10;
    GENERATE FLATTEN(top10);
};
top10_reviewed_by_rating = FOREACH top10_reviewed_ratings GENERATE rating, app_id, title, user_reviews;
STORE top10_reviewed_by_rating INTO '/steam/pig/Top10ReviewedByRating' USING PigStorage('\t');
