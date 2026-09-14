# Steam Game Recommendations Analysis — MapReduce Design Patterns using Apache Hadoop and Pig

**Big Data Analysis Laboratory (CSE 4346) — final project**

This project analyses a 2.2 GB Steam dataset of 41 million user reviews on an Apache Hadoop cluster.
The data is stored in HDFS and processed by YARN, first with three Java MapReduce programs — each
written on a different MapReduce design pattern — and then with five Apache Pig Latin scripts that
ask ranking and grouping questions of the same data. Every Pig result is charted, and the answers the
two engines share are compared against each other to confirm both are correct.

- **Cluster:** single-node Apache Hadoop 3.4.1 (HDFS + YARN), Apache Pig 0.18.0, OpenJDK 11.
- **MapReduce:** summarization with a custom `Writable` and a combiner, data organization (binning)
  with `MultipleOutputs`, and a reduce-side join with `MultipleInputs`.
- **Pig:** five scripts, including a map-side replicated join over the full 41-million-row review table.
- **Report:** [`report/SteamDataAnalysisProject.pdf`](report/SteamDataAnalysisProject.pdf) — the lab
  report, written in LaTeX.

## Repository structure

```
Steam-Data-Analysis-using-Hadoop/
├── dataset/                  schema and download instructions for the Steam dataset
├── GameReviewSummary/        MapReduce 1 — review summary per game (summarization)
├── BinningByRating/          MapReduce 2 — binning games by rating (data organization)
├── GameReviewJoin/           MapReduce 3 — games joined with their reviews (reduce-side join)
├── PigAnalysis/scripts/      the five Pig Latin scripts
├── MR Results/outputFiles/   MapReduce job outputs, read back from HDFS
├── Pig Results/outputFiles/  Pig script outputs, read back from HDFS
├── screenshots/              cluster, job runs, outputs and the Hadoop web interfaces
├── visualizations/           charts of the Pig results, and the script that draws them
├── logs/                     complete console output of every command that was run
├── scripts/build.sh          compiles the three MapReduce jobs into jars
└── report/                   the lab report and its LaTeX source
```

| Folder | What is inside |
|---|---|
| `dataset/` | The field-by-field schema of the three tables and the command to download them from Kaggle. The data files are not committed. |
| `GameReviewSummary/` | Four Java classes (driver, mapper, reducer/combiner and a custom `Writable` tuple) under `src/main/java/steam/summary/`, plus the built jar. |
| `BinningByRating/` | Two Java classes (driver and mapper) under `src/main/java/steam/binning/`, plus the built jar. |
| `GameReviewJoin/` | Four Java classes (driver, two mappers and the join reducer) under `src/main/java/steam/join/`, plus the built jar. |
| `PigAnalysis/scripts/` | Five `.pig` scripts, one per analysis, run with `pig -x mapreduce -f <script>`. |
| `MR Results/outputFiles/` | The output of each MapReduce job. For the binning job only the first 20 lines of each bin are kept, because the bins together are a full copy of `games.csv`. |
| `Pig Results/outputFiles/` | The stored output of each of the five Pig scripts. |
| `screenshots/` | 43 screenshots: the running cluster, the data in HDFS, every job run and its output, and the YARN, JobHistory and HDFS web pages. |
| `visualizations/` | Five charts built from the Pig results by `make_charts.py`. |
| `logs/` | The full console output of all 21 recorded commands, from starting the cluster to the last Pig job. |
| `report/` | `SteamDataAnalysisProject.pdf` and the LaTeX source it is built from, with the cover page. |
| `scripts/` | `build.sh`, which compiles each job against the local Hadoop classpath and packages it as a jar. |

## Problem Statement

Analyse the Steam game-recommendations dataset using Hadoop (MapReduce) and Pig, across its game and
review tables, to find which games and rating groups draw the most reviews and the most hours of play.

## Dataset

The dataset is available at:

https://www.kaggle.com/datasets/antonkozyriev/game-recommendations-on-steam

It has three related tables, comma separated with a header row and no quoted fields:

| File | Rows | One row is |
|---|---:|---|
| `recommendations.csv` (2.0 GB) | 41,154,794 | one user review of one game |
| `users.csv` (193 MB) | 14,306,064 | one Steam user |
| `games.csv` (4.9 MB) | 50,872 | one game on the Steam store |

**games.csv** — `app_id`, `title`, `date_release`, `win`, `mac`, `linux`, `rating` (Steam's review
label, 9 values), `positive_ratio`, `user_reviews`, `price_final`, `price_original`, `discount`,
`steam_deck`

**recommendations.csv** — `app_id` (→ games), `helpful`, `funny`, `date`, `is_recommended`, `hours`,
`user_id` (→ users), `review_id`

**users.csv** — `user_id`, `products`, `reviews`

Field-by-field schema with types and examples: [`dataset/README.md`](dataset/README.md). The data files
themselves are not committed.

Following MapReduce design patterns are implemented:
1. Summarization
2. Data Organization (binning)
3. Join Patterns (reduce-side join)

Following MapReduce analysis is performed on the dataset:
1. Review summary per game — reviews, recommended share, total / average / max hours, helpful votes
2. Binning games by rating — one output file per Steam rating label
3. Games joined with their reviews — title and rating next to review count and hours played

Following Pig analysis is performed on the dataset (visualizations are in the Pig section below):
1. Top 5 rating groups by number of games
2. Top 10 most reviewed games
3. Top 10 most reviewed games in each rating group
4. Top 10 games by total hours played
5. Top 10 games by total hours played in each rating group

## Environment

| Component | Version / setting |
|---|---|
| Apache Hadoop | 3.4.1, pseudo-distributed (single node), replication 1, 128 MB blocks |
| Apache Pig | 0.18.0, `mapreduce` mode |
| Java | OpenJDK 11.0.31 |
| YARN memory | NodeManager 5120 MB; map 768 MB, reduce 1024 MB, AM 768 MB |

![jps](screenshots/01_cluster_jps.png)

Loading the data into HDFS — the 2 GB review file is stored as 16 blocks:

```bash
hdfs dfs -mkdir -p /steam
cd dataset && hdfs dfs -put -f games.csv users.csv recommendations.csv /steam/
```

![hdfs ls](screenshots/04_hdfs_ls.png)

Building the three jobs (one jar each, compiled against the local Hadoop classpath):

```bash
./scripts/build.sh
```

## Map Reduce Analysis

### 1. Review Summary per Game — *summarization* ([`GameReviewSummary/`](GameReviewSummary/))

Returns, for every game, how many reviews it received, how many recommend it, how long its reviewers
played it and how many helpful votes the reviews collected.

1. **Driver Class** — sets up the job and registers the reducer as the combiner too
2. **Mapper Class** — emits `app_id` (`Text`) and a `ReviewStatsTuple` with a count of one
3. **Reducer Class** — merges all tuples of one game; every field is a count, sum or max, so the same
   class is a valid combiner
4. **Tuple Class** — custom `Writable`: reviews, recommended, total hours, max hours, helpful votes

```bash
hadoop jar GameReviewSummary/GameReviewSummary.jar steam.summary.GameReviewSummary /steam/recommendations.csv /steam/out_summary
```

41,154,794 reviews in, 37,610 games out, in 2 min 15 s. The combiner cut the shuffle from 41 million
records to 90,766.

![MR1 counters](screenshots/MR1_counters.png)
![MR1 output](screenshots/MR1_output.png)

### 2. Binning Games by Rating — *data organization* ([`BinningByRating/`](BinningByRating/))

Map-only job: every game is written to the output file of its rating label through
`MultipleOutputs` (`VeryPositive-m-00000`, `Mixed-m-00000`, …), with a counter per bin.

1. **Driver Class** — zero reducers, registers the `bins` named output, `LazyOutputFormat`
2. **Mapper Class** — writes each game to its bin and counts it

```bash
hadoop jar BinningByRating/BinningByRating.jar steam.binning.BinningByRating /steam/games.csv /steam/out_binning
```

![MR2 counters](screenshots/MR2_counters.png)
![MR2 output](screenshots/MR2_output.png)

| Bin | Games | | Bin | Games |
|---|---:|---|---|---:|
| Positive | 13,502 | | Overwhelmingly Positive | 1,110 |
| Very Positive | 13,139 | | Negative | 303 |
| Mixed | 12,157 | | Very Negative | 60 |
| Mostly Positive | 8,738 | | Overwhelmingly Negative | 14 |
| Mostly Negative | 1,849 | | **Total** | **50,872** |

### 3. Games Joined with their Reviews — *reduce-side join* ([`GameReviewJoin/`](GameReviewJoin/))

Two inputs, each with its own mapper (`MultipleInputs`), meet in the reducer on `app_id`. Values are
tagged `G` (game) or `R` (review totals). The review mapper uses in-mapper combining — one record per
game per split instead of one per review.

1. **Driver Class** — registers both inputs with their mappers
2. **Game Mapper** — emits `app_id` → `G, title, rating`
3. **Recommendation Mapper** — totals reviews, recommendations and hours per game in memory
4. **Join Reducer** — matches the `G` and `R` records and writes the joined line

```bash
hadoop jar GameReviewJoin/GameReviewJoin.jar steam.join.GameReviewJoin /steam/games.csv /steam/recommendations.csv /steam/out_join
```

37,610 games joined, 13,262 games with no reviews, in 1 min 15 s.

![MR3 counters](screenshots/MR3_counters.png)
![MR3 output](screenshots/MR3_output.png)

Job outputs: [`MR Results/outputFiles/`](MR%20Results/outputFiles/) (for the binning job only the
first 20 lines of each bin are kept, since the bins together are a full copy of `games.csv`).

## Pig Analysis

Scripts: [`PigAnalysis/scripts/`](PigAnalysis/scripts/) — run with `pig -x mapreduce -f <script>`.
Outputs: [`Pig Results/outputFiles/`](Pig%20Results/outputFiles/).

### 1) Top 5 Rating Groups

`GROUP BY rating` → `COUNT` → nested `ORDER … LIMIT 5`. Same five counts as the binning job.

![Pig1 output](screenshots/Pig1_output.png)
![Pig1 chart](visualizations/pig1_top5_ratings.png)

### 2) Top 10 Most Reviewed Games

![Pig2 output](screenshots/Pig2_output.png)
![Pig2 chart](visualizations/pig2_top10_reviewed.png)

### 3) Top 10 Most Reviewed Games in each Rating Group

Nested `FOREACH { ORDER; LIMIT; GENERATE FLATTEN }` per rating group — 90 rows.

![Pig3 chart](visualizations/pig3_top10_reviewed_by_rating.png)

### 4) Top 10 Games by Total Hours Played

Sums the hours of all 41 million reviews per game, then a replicated (map-side) join with
`games.csv`. Identical to the top ten of the MapReduce join.

![Pig4 result](screenshots/Pig4_result.png)
![Pig4 output](screenshots/Pig4_output.png)
![Pig4 chart](visualizations/pig4_top10_played.png)

### 5) Top 10 Games by Total Hours Played in each Rating Group

87 rows: the Overwhelmingly Negative group has only 7 games with any reviews.

![Pig5 chart](visualizations/pig5_top10_played_by_rating.png)

Charts are drawn by [`visualizations/make_charts.py`](visualizations/make_charts.py) from the Pig outputs.

## Monitoring

![YARN applications](screenshots/web_yarn_applications.png)
![JobHistory](screenshots/web_jobhistory.png)

## Verification

| Check | Result |
|---|---|
| Reviews read by job 1 = rows of `recommendations.csv` (41,154,794) | pass |
| Reviews summed over job 1 output = over job 3 output | pass |
| Games in job 1 output = games joined by job 3 (37,610); + 13,262 unreviewed = 50,872 | pass |
| Nine bins add up to 50,872 and match counts taken directly from the CSV | pass |
| Pig top 5 rating groups = five largest bins | pass |
| Pig top 10 by hours = MapReduce join top 10 (ids, reviews, hours) | pass |

*Rimjhim Dey*
