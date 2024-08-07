// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.vcs.log.impl;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.vcs.log.Hash;
import com.intellij.vcs.log.TimedVcsCommit;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * <p>We suppose that the Hash is the unique identifier of the Commit,
 * i. e. it is the only value that should be checked in equals() and hashCode().</p>
 * <p>equals() and hashCode() are made final to ensure that any descendants of this class are considered equal
 * if and only if their hashes are equals.</p>
 * <p>It is highly recommended to use this standard implementation of the VcsCommit because of the above reasons.</p>
 *
 * @author erokhins
 * @author Kirill Likhodedov
 */
public class TimedVcsCommitImpl implements TimedVcsCommit {

  private final @NotNull Hash myHash;
  private final @NotNull List<Hash> myParents;
  private final long myTime;

  public TimedVcsCommitImpl(@NotNull Hash hash, @NotNull List<Hash> parents, long timeStamp) {
    myHash = hash;
    myParents = parents;
    myTime = timeStamp;
  }

  @Override
  public final @NotNull Hash getId() {
    return myHash;
  }

  @Override
  public final @NotNull List<Hash> getParents() {
    return myParents;
  }

  @Override
  public final boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof TimedVcsCommitImpl)) {
      return false;
    }
    return myHash.equals(((TimedVcsCommitImpl)obj).myHash);
  }

  @Override
  public final int hashCode() {
    return myHash.hashCode();
  }

  @Override
  public String toString() {
    return myHash.toShortString() + "|-" + StringUtil.join(ContainerUtil.map(myParents, hash -> hash.toShortString()), ",") + ":" + myTime;
  }

  @Override
  public final long getTimestamp() {
    return myTime;
  }
}
