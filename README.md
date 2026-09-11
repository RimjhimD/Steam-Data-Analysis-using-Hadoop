# Steam Game Recommendations — Data Analysis using Apache Hadoop and Pig

Big Data Analysis Laboratory (CSE 4346) final project.

- Analysed a 2 GB Steam game-recommendations dataset on a single-node Apache Hadoop 3.4.1 cluster
  with HDFS and YARN.
- Wrote three Java MapReduce programs, each built on a different MapReduce design pattern.
- Wrote five Apache Pig Latin scripts for ranking and grouping queries on the same data.

> Work in progress — results, screenshots and the report are added as each job is run.

### Problem Statement

Analyse the Steam game-recommendations dataset with Hadoop MapReduce and Apache Pig across its
game, user and recommendation fields to find which games, rating groups and players drive activity on
the platform.

### Summary

The dataset has three related tables joined on `app_id` and `user_id`:

| File | Rows | What one row is |
|---|---:|---|
| `games.csv` | 50,872 | a game on the Steam store |
| `users.csv` | 14,306,064 | a Steam user |
| `recommendations.csv` | 41,154,794 | one user review of one game |

Field-by-field schema: [`dataset/README.md`](dataset/README.md).

### Repository layout

```
├── dataset/           schema and source (the data files themselves are not committed)
├── MapReduce/         one folder per MapReduce job: Java source + how to run it
├── PigAnalysis/       the five Pig scripts
├── output/            results pulled back out of HDFS
├── screenshots/       cluster, job runs and outputs
├── visualizations/    charts built from the Pig results
├── report/            LaTeX lab report
└── scripts/           build and run helpers
```
