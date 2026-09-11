#!/usr/bin/env python3
"""Draw one chart per Pig analysis from the outputs in "Pig Results/outputFiles".

usage: python3 visualizations/make_charts.py            (needs matplotlib)
       python3 visualizations/make_charts.py --print    black-and-white copies in visualizations/print/
"""
import csv
import glob
import os
import sys

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.ticker import FuncFormatter

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RESULTS = os.path.join(ROOT, "Pig Results", "outputFiles")
PRINT = "--print" in sys.argv
OUT = os.path.join(ROOT, "visualizations", "print") if PRINT else os.path.join(ROOT, "visualizations")
os.makedirs(OUT, exist_ok=True)

BAR = "#555555" if PRINT else "#2a78d6"   # one hue: every chart here is a single series
INK = "#000000" if PRINT else "#0b0b0b"
INK_2 = "#333333" if PRINT else "#52514e"
GRID = "#cccccc" if PRINT else "#e4e3df"
SURFACE = "#ffffff"

RATING_ORDER = ["Overwhelmingly Positive", "Very Positive", "Positive", "Mostly Positive",
                "Mixed", "Mostly Negative", "Negative", "Very Negative",
                "Overwhelmingly Negative"]

plt.rcParams.update({
    "font.family": ["DejaVu Sans", "Noto Sans CJK JP"], "font.size": 10, "text.color": INK,
    "axes.edgecolor": GRID, "axes.labelcolor": INK_2, "xtick.color": INK_2,
    "ytick.color": INK, "axes.titleweight": "bold", "axes.titlesize": 12,
    "figure.facecolor": SURFACE, "axes.facecolor": SURFACE, "savefig.dpi": 200,
})


def rows(name):
    out = []
    for part in sorted(glob.glob(os.path.join(RESULTS, name, "part-*"))):
        with open(part, encoding="utf-8") as fh:
            out += [r for r in csv.reader(fh, delimiter="\t") if r]
    return out


def short(title, n=34):
    return title if len(title) <= n else title[: n - 1] + "…"


def human(v, _=None):
    if v >= 1e6:
        return f"{v / 1e6:.0f}M" if v >= 1e7 else f"{v / 1e6:.1f}M"
    if v >= 1e4:
        return f"{v / 1e3:.0f}K"
    if v >= 1e3:
        return f"{v / 1e3:.1f}".rstrip("0").rstrip(".") + "K"
    return f"{v:.0f}"


def style(ax):
    ax.spines[["top", "right", "left"]].set_visible(False)
    ax.grid(axis="x", color=GRID, linewidth=0.8)
    ax.set_axisbelow(True)
    ax.tick_params(axis="y", length=0)
    ax.xaxis.set_major_formatter(FuncFormatter(human))


def hbar(ax, labels, values, fmt=human):
    """Ranked horizontal bars, largest on top, value written at the bar end."""
    y = range(len(values))[::-1]
    ax.barh(list(y), values, color=BAR, height=0.72, edgecolor=SURFACE, linewidth=1)
    ax.set_yticks(list(y), labels)
    top = max(values)
    for yi, v in zip(y, values):
        ax.text(v + top * 0.01, yi, fmt(v), va="center", fontsize=8.5, color=INK_2)
    ax.set_xlim(0, top * 1.12)
    style(ax)


def save(fig, name):
    fig.savefig(os.path.join(OUT, name), bbox_inches="tight")
    plt.close(fig)
    print("wrote", name)


# Pig-1  top 5 rating groups by number of games
r = rows("Top5Ratings")
fig, ax = plt.subplots(figsize=(8, 3.2))
hbar(ax, [x[0] for x in r], [int(x[1]) for x in r], fmt=lambda v: f"{v:,.0f}")
ax.set_title("Top 5 rating groups by number of games", loc="left")
ax.set_xlabel("games")
save(fig, "pig1_top5_ratings.png")

# Pig-2  top 10 most reviewed games (Steam user_reviews)
r = rows("Top10Reviewed")
fig, ax = plt.subplots(figsize=(8, 4.6))
hbar(ax, [short(x[1]) for x in r], [int(x[4]) for x in r])
ax.set_title("Top 10 most reviewed games", loc="left")
ax.set_xlabel("user reviews on Steam")
save(fig, "pig2_top10_reviewed.png")


def small_multiples(data, value_col, title, xlabel, name):
    groups = {}
    for x in data:
        groups.setdefault(x[0], []).append(x)
    fig, axes = plt.subplots(3, 3, figsize=(15, 13))
    for ax, rating in zip(axes.flat, RATING_ORDER):
        g = sorted(groups.get(rating, []), key=lambda x: -float(x[value_col]))
        hbar(ax, [short(x[2], 26) for x in g], [float(x[value_col]) for x in g])
        ax.set_title(rating, loc="left", fontsize=11)
        ax.tick_params(axis="y", labelsize=8.5)
        ax.tick_params(axis="x", labelsize=8)
    fig.suptitle(title, x=0.01, ha="left", fontsize=14, fontweight="bold")
    fig.supxlabel(xlabel + "  (each panel has its own scale)", color=INK_2, fontsize=10)
    fig.tight_layout(rect=(0, 0.01, 1, 0.97))
    save(fig, name)


# Pig-3  top 10 most reviewed inside each rating group: rating, app_id, title, user_reviews
small_multiples(rows("Top10ReviewedByRating"), 3,
                "Top 10 most reviewed games in each rating group", "user reviews on Steam",
                "pig3_top10_reviewed_by_rating.png")

# Pig-4  top 10 by total hours played: app_id, title, rating, reviews, hours
r = rows("Top10Played")
fig, ax = plt.subplots(figsize=(8, 4.6))
hbar(ax, [short(x[1]) for x in r], [float(x[4]) for x in r])
ax.set_title("Top 10 games by total hours played by reviewers", loc="left")
ax.set_xlabel("hours played, summed over all reviewers")
save(fig, "pig4_top10_played.png")

# Pig-5  top 10 by hours inside each rating group: rating, app_id, title, reviews, hours
small_multiples(rows("Top10PlayedByRating"), 4,
                "Top 10 games by total hours played in each rating group",
                "hours played, summed over all reviewers", "pig5_top10_played_by_rating.png")
