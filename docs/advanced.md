---
layout: default
title: Advanced Usage
nav_order: 90
---

# Advanced Usage

This page assumes you already know how to use git.  If you don't, you should skip it.

## Disabling file updates

FastBack automatically performs updates to some key git files in your world repo.  Advanced users
can disable these updates by changing the repo's git configuration:

| Config Key                              | Use                                                                                           |
|-----------------------------------------|-----------------------------------------------------------------------------------------------|
| `fastback.update-gitignore-enabled`     | Defaults to `true`.  Set to `false` to disable automatic updates to the root `.gitignore`     |
| `fastback.update-gitattributes-enabled` | Defaults to `true`.  Set to `false` to disable automatic updates to the root `.gitattributes` |

For example, running
```
git config fastback.update-gitignore-enabled false
```
in your world folder will disable `.gitignore` updates.  

Be aware that you might miss out on future optimizations or bug fixes in these files; by disabling the 
updates, you take full responsibility for maintaining them.  As an alternative, you might want to consider 
adding custom `.gitignore` or `.gitattributes` files in subdirectories and letting FastBack continue to
auto-update the root files.
