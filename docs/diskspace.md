---
layout: default
title: Disk Space
nav_order: 30
---

# Managing Disk Space

FastBack makes it easy for you to store lots of snapshots of your world.  They save quickly
and don't use up too much disk space.

But eventually, you'll probably want to get rid of older snapshots you don't need anymore so
you can get some disk space back.


## Pruning Snapshots

To do this, you can run the `prune` command to delete old snapshots that you don't need anymore.

```
/backup prune
```

By default, this will retain the last snapshot from each day, plus all snapshots
from the last 3 days.  All other snapshots will be removed.

## Changing How Snapshots are Retained

You can change the rules for retaining snapshots by running `set-retention`:

```
/backup set-retention [policy] [arguments...]
```

Where `[policy]` is one of

{% include_relative retention-list.md %}

For example, to change the policy to keep the five most-recent snapshots, run:

```
/backup set-retention count 5
```


## Collecting Garbage

The `prune` command marks the snapshots as unused but does not delete from disk.
To actually delete the snapshots and reclaim the disk space they occupy, you need to

```
/backup gc
```

Note that this command can take a long time (5+ minutes) if your world is large or you 
haven't run it in a while.


## Managing Snapshots on a Remote

The commands above apply only to *local* snapshots.  FastBack does not currently
offer the ability to manage snapshots on a remote repo.  This will be added
in a future release; see [Issues](https://github.com/pcal43/fastback/issues).
