---
layout: default
title: Disk Space
nav_order: 30
---

# Managing Disk Space

FastBack makes it easy for you to store lots of snapshots of your world. They save quickly
and don't use up too much disk space.

But eventually, you'll probably want to get rid of older snapshots you don't need anymore so
you can get some disk space back.

## Pruning Snapshots

To do this, you can run the `prune` command to delete old snapshots that you don't need anymore.

```
/backup prune
```

By default, this will retain the last snapshot from each day, plus all snapshots
from the last 3 days. All other snapshots will be removed.

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

You can also manage snapshots on a remote backup on a similar way using the
`set-remote-retention` and `remote-prune` commands. For example,

```
/backup set-remote-retention daily 7
```

will set the retention policy for snapshots in the remote backup to keep all snapshots for the last 7 days
and at most one snapshot per day before that. Then, you can prune old snapshots on the remote by
running

```
/backup remote-prune
```

**Note:** it is *not* possible to perform garbage collection on the remote using minecraft commands; you have
to run it directly on the server. Many git servers will do this automatically for you but it depends on which
server you're using and how it's configured.
