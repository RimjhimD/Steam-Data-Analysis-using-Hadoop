# Dataset

**Game Recommendations on Steam** — Kaggle, `antonkozyriev/game-recommendations-on-steam`
(<https://www.kaggle.com/datasets/antonkozyriev/game-recommendations-on-steam>).

The data files are not committed to this repository. Download the archive from Kaggle and unzip it
into this folder:

```bash
curl -L -o steam.zip https://www.kaggle.com/api/v1/datasets/download/antonkozyriev/game-recommendations-on-steam
unzip steam.zip && rm steam.zip
```

| File | Size | Rows (excluding header) |
|---|---:|---:|
| `recommendations.csv` | 2.0 GB | 41,154,794 |
| `users.csv` | 193 MB | 14,306,064 |
| `games.csv` | 4.9 MB | 50,872 |
| `games_metadata.json` | 18 MB | 50,872 (one JSON object per line) |

All three CSV files are comma separated with a header row and no quoted fields; every row has the
expected number of columns.

## games.csv

| # | Field | Type | Example |
|---:|---|---|---|
| 0 | `app_id` | int | `13500` |
| 1 | `title` | string | `Prince of Persia: Warrior Within™` |
| 2 | `date_release` | date | `2008-11-21` |
| 3 | `win` | boolean | `true` |
| 4 | `mac` | boolean | `false` |
| 5 | `linux` | boolean | `false` |
| 6 | `rating` | string, 9 values | `Very Positive` |
| 7 | `positive_ratio` | int, % | `84` |
| 8 | `user_reviews` | int | `2199` |
| 9 | `price_final` | float, USD | `9.99` |
| 10 | `price_original` | float, USD | `9.99` |
| 11 | `discount` | float, % | `0.0` |
| 12 | `steam_deck` | boolean | `true` |

`rating` is Steam's review summary label. Games per label:

| rating | games |
|---|---:|
| Positive | 13,502 |
| Very Positive | 13,139 |
| Mixed | 12,157 |
| Mostly Positive | 8,738 |
| Mostly Negative | 1,849 |
| Overwhelmingly Positive | 1,110 |
| Negative | 303 |
| Very Negative | 60 |
| Overwhelmingly Negative | 14 |

## users.csv

| # | Field | Type | Meaning |
|---:|---|---|---|
| 0 | `user_id` | int | anonymised user id |
| 1 | `products` | int | games the user owns |
| 2 | `reviews` | int | reviews the user has written |

## recommendations.csv

| # | Field | Type | Meaning |
|---:|---|---|---|
| 0 | `app_id` | int | game reviewed → `games.app_id` |
| 1 | `helpful` | int | users who marked the review helpful |
| 2 | `funny` | int | users who marked the review funny |
| 3 | `date` | date | review date |
| 4 | `is_recommended` | boolean | thumbs up / thumbs down |
| 5 | `hours` | float | hours the reviewer had played |
| 6 | `user_id` | int | reviewer → `users.user_id` |
| 7 | `review_id` | int | unique review id |
