#!/usr/bin/env bash
# Compile each MapReduce job against the local Hadoop and package it as
# <Job>/<Job>.jar. Needs `hadoop` on PATH (Hadoop 3.4.1, Java 11).
set -euo pipefail
cd "$(dirname "$0")/.."

for job in GameReviewSummary BinningByRating GameReviewJoin; do
    rm -rf "build/$job" && mkdir -p "build/$job"
    javac -cp "$(hadoop classpath)" -d "build/$job" $(find "$job/src/main/java" -name '*.java')
    jar cf "$job/$job.jar" -C "build/$job" .
    echo "built $job/$job.jar"
done
