---
layout: default
title: Scheduling
nav_order: 20
---

# Scheduling

You can schedule backups to run automatically when the world shuts down or auto-saves.

## Backing up on shutdown

Backups can be run whenever the world shuts down (i.e., when exiting a single-player world or
shutting down a dedicated server). To do this,

```
/backup set-shutdown-action [action]
```

Where `[action]` is one of

{% include_relative actions-list.md %}

## Backing up while the game is running

You can also set backups to run while the game is playing, immediately after the
regular auto-saves that Minecraft performs every 5 minutes.  To do this,

```
/backup set-autoback-action [action]
```

Where `[action]` is one of the actions listed above.

If you don't want `autoback` backups to run every 5 minutes, you can schedule them to
run less-frequently:

```
/backup set-autoback-wait [minutes]
```

This sets the minimum wait time between auto-backups.

So, for example, setting `[minutes]` 
to 120 will cause backups to run *roughly* every two hours; the exact timing will depend 
on when the next autosave runs.
