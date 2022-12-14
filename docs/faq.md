---
layout: default
title: FAQ
nav_order: 99
---

# FAQ

## What is an Incremental Backup?

Say you're playing for a few hours in a Minecraft world that takes up 5GB but you spend the whole time
just working on your base.  Minecraft only changes files for the parts of the world you changed, which might
only be 50 or 100MB of files.  The other 4.9GB is completely the same as it was before you started playing.

When you're done playing, wouldn't it be nice to back up only the parts of the world that changed, without 
making a whole new copy of all the stuff that didn't change?

That's what FastBack does.

## How big of a world can I back up?

FastBack is designed for worlds up to about 5GB.  It may work ok with worlds larger than that; please give it a 
try and let us know!

But if you're running a server with a 200GB world, you're probably better off sticking with rsync (or whatever
you're using).

## I just turned it on and it's taking a while.  I thought you said this thing was fast?

The first time you back up, it's going to take a while to establish a 'base' snapshot.  The *next* time you 
back up on top of that base, it will be a lot faster.

## Where are the backups stored?

The backups are stored inside your world folder, in a secret directory called `.git`. You won't see any files
in there that you recognize; to get your backups out, you need to use the `/backup restore` command.

## Why did my world folder get so much bigger?

The first time you do a backup, all the files are backed up in the world folder under `.git`.  But the next
time you back up, only changed files will be backed up.  See question above about incremental backups.

*Technical detail: It's just a regular git repository, no shenanigans.*

## Can I back up my world to github?

You can do a remote backup to any git server, including github.

But because github has certain [size restrictions](https://docs.github.com/en/repositories/working-with-files/managing-large-files/about-large-files-on-github),
we'd only recommend using it for smaller worlds (under 500MB).

## I thought git was bad for backups?

Git is a popular source code management tool used by software developers.  And it's *really* good at storing text
files, such as program source code. 

But when it comes to binary files - images or, say, minecraft region files - git's text file magic doesn't work.
And software developers can run into a lot of problems if they carelessly mix binary files with source code in a 
git repo.

For this reason, most people think that "git is bad for binary files."  But with careful handling, git can actually 
be used to store just about anything.

Technical detail: FastBack disables delta compression, stores each backup snapshot in an orphan branch and 
aggressively prunes reflogs and tracking branches.*
