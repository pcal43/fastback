---
layout: default
title: Native Git Support
nav_order: 95
---

# Native Git

As of version `0.17.1`, Fastback **requires** that you have native `git` and `git-lfs` installed on your machine.

## Installing Native Git

Installing git is highly dependent on your platform but there are tons of resources on the web describing how to do it. 
Here are some good places to start:

* Installing [git](https://github.com/git-lfs/git-lfs/wiki/Installation)
* Installing [git-lfs](https://docs.github.com/en/repositories/working-with-files/managing-large-files/installing-git-large-file-storage)

Note that both git and git-lfs must be available on the `PATH` of the Minecraft process.

## How do I know if Native Git has been installed correctly?

You can check the minecraft startup logs for lines that look like this
```
[14:39:01] [Render thread/INFO] (fastback) git is installed: git version 2.43.0
[14:39:01] [Render thread/WARN] (fastback) git-lfs is not installed.
```

You can also type `/backup info` into the chat to check.


## Why is native git required now?


## Older Backups

Backups created prior to version 0.17.1 in non-native mode enabled will continue to function with jgit (and in fact
are incompatible with native mode in some ways).

## Why is native git required now?

Non-native mode relied on java-based re-implementation of git called JGit.  While JGit is an impressive piece of
engineering, it has proven to have some annoying differences from native git and is also much less performant.  And
it's just become too burdensome to support two modes, especially when one of them is unreliable.

Requiring native git also ensures that backups will always be manageable with the standard git command-line tool 
(which is not a guarantee with jgit). 

## I can't figure out how to get native git installed.  What do I do?

This is understandable if you're new to git and/or system administration.  You can ask on the discord channel
for help.  But to be completely honest, if you find this process daunting, you might want to consider other
backup options.
