// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.tasks.generic;

import com.intellij.tasks.Comment;
import com.intellij.tasks.Task;
import com.intellij.tasks.TaskRepository;
import com.intellij.tasks.TaskType;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Date;

public class GenericTask extends Task {
  private final String myId;
  private final @Nls String mySummary;
  private @Nls String myDescription;
  private Date myUpdated;
  private Date myCreated;
  private String myIssueUrl;
  private final TaskRepository myRepository;
  private boolean myClosed;

  public GenericTask(final String id, final @Nls String summary, final TaskRepository repository) {
    myId = id;
    mySummary = summary;
    myRepository = repository;
  }

  @Override
  public @NotNull String getId() {
    return myId;
  }

  @Override
  public @NotNull String getSummary() {
    return mySummary;
  }

  @Override
  public @Nullable String getDescription() {
    return myDescription;
  }

  @Override
  public Comment @NotNull [] getComments() {
    return Comment.EMPTY_ARRAY;
  }

  @Override
  public @NotNull Icon getIcon() {
    return myRepository.getIcon();
  }

  @Override
  public @NotNull TaskType getType() {
    return TaskType.OTHER;
  }

  @Override
  public @Nullable Date getUpdated() {
    return myUpdated;
  }

  @Override
  public @Nullable Date getCreated() {
    return myCreated;
  }

  @Override
  public boolean isClosed() {
    return myClosed;
  }

  @Override
  public boolean isIssue() {
    return true;
  }

  @Override
  public @Nullable String getIssueUrl() {
    return myIssueUrl;
  }

  @Override
  public @Nullable TaskRepository getRepository() {
    return myRepository;
  }

  public void setIssueUrl(@Nullable String issueUrl) {
    myIssueUrl = issueUrl;
  }

  public void setCreated(@Nullable Date created) {
    myCreated = created;
  }

  public void setUpdated(@Nullable Date updated) {
    myUpdated = updated;
  }

  public void setDescription(@Nullable @Nls String description) {
    myDescription = description;
  }

  public void setClosed(boolean closed) {
    myClosed = closed;
  }
}