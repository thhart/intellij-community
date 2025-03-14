// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.vcs.impl;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.*;
import com.intellij.openapi.vcs.changes.*;
import com.intellij.openapi.vcs.diff.DiffProvider;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vcs.impl.VcsBaseContentProvider.BaseContent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsImplUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class LineStatusTrackerBaseContentUtil {
  private static final Logger LOG = Logger.getInstance(LineStatusTrackerBaseContentUtil.class);

  private static @Nullable VcsBaseContentProvider findProviderFor(@NotNull Project project, @NotNull VirtualFile file) {
    return VcsBaseContentProvider.EP_NAME.findFirstSafe(project, it -> it.isSupported(file));
  }

  public static boolean isSupported(@NotNull Project project, @NotNull VirtualFile file) {
    return isHandledByVcs(project, file) || findProviderFor(project, file) != null;
  }

  public static boolean isTracked(@NotNull Project project, @NotNull VirtualFile file) {
    FileStatus status = FileStatusManager.getInstance(project).getStatus(file);
    if (status == FileStatus.ADDED ||
        status == FileStatus.DELETED ||
        status == FileStatus.UNKNOWN ||
        status == FileStatus.IGNORED) {
      return false;
    }
    return true;
  }

  private static boolean isHandledByVcs(@NotNull Project project, @NotNull VirtualFile file) {
    return file.isInLocalFileSystem() && ProjectLevelVcsManager.getInstance(project).getVcsFor(file) != null;
  }

  public static @Nullable BaseContent getBaseRevision(@NotNull Project project, @NotNull VirtualFile file) {
    if (!isHandledByVcs(project, file)) {
      if (LOG.isDebugEnabled()) {
        LOG.debug(String.format("File is not under VCS: %s", file));
      }
      return createFromProvider(project, file);
    }

    ChangeListManager changeListManager = ChangeListManager.getInstance(project);

    Change change = changeListManager.getChange(file);
    if (change != null) {
      if (LOG.isDebugEnabled()) {
        LOG.debug(String.format("Content by Change %s: %s", change, file));
      }
      return createFromLocalChange(project, change);
    }

    FileStatus status = changeListManager.getStatus(file);
    if (LOG.isDebugEnabled()) {
      LOG.debug(String.format("File status is %s: %s", status, file));
    }

    if (status == FileStatus.HIJACKED) {
      return createForHijacked(project, file);
    }
    if (status == FileStatus.NOT_CHANGED) {
      if (FileDocumentManager.getInstance().isFileModified(file)) {
        if (LOG.isDebugEnabled()) {
          LOG.debug(String.format("Document is modified: %s", file));
        }
        return createForModifiedDocument(project, file);
      }
    }
    return null;
  }

  private static @Nullable BaseContent createFromProvider(@NotNull Project project, @NotNull VirtualFile file) {
    VcsBaseContentProvider provider = findProviderFor(project, file);
    if (provider == null) return null;

    if (LOG.isDebugEnabled()) {
      LOG.debug(String.format("Content by provider %s: %s", provider, file));
    }
    return provider.getBaseRevision(file);
  }

  private static @Nullable BaseContent createFromLocalChange(@NotNull Project project, @NotNull Change change) {
    ContentRevision beforeRevision = change.getBeforeRevision();
    if (beforeRevision == null) return null;
    return createBaseContent(project, beforeRevision);
  }

  private static @Nullable BaseContent createForHijacked(@NotNull Project project, @NotNull VirtualFile file) {
    AbstractVcs vcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(file);
    if (vcs == null) return null;
    DiffProvider diffProvider = vcs.getDiffProvider();
    if (diffProvider == null) return null;

    VcsRevisionNumber currentRevision = diffProvider.getCurrentRevision(file);
    if (currentRevision == null) return null;
    return new HijackedBaseContent(project, diffProvider, file, currentRevision);
  }

  private static @Nullable BaseContent createForModifiedDocument(@NotNull Project project, @NotNull VirtualFile file) {
    AbstractVcs vcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(file);
    if (vcs == null) return null;
    DiffProvider diffProvider = vcs.getDiffProvider();
    ChangeProvider cp = vcs.getChangeProvider();
    if (diffProvider == null || cp == null) return null;

    if (!cp.isModifiedDocumentTrackingRequired()) return null;

    ContentRevision beforeRevision = diffProvider.createCurrentFileContent(file);
    if (beforeRevision == null) return null;
    return createBaseContent(project, beforeRevision);
  }

  public static @NotNull BaseContent createBaseContent(@NotNull Project project, @NotNull ContentRevision contentRevision) {
    return new BaseContentImpl(project, contentRevision);
  }

  private static class BaseContentImpl implements BaseContent {
    private final @NotNull Project myProject;
    private final @NotNull ContentRevision myContentRevision;

    BaseContentImpl(@NotNull Project project, @NotNull ContentRevision contentRevision) {
      myProject = project;
      myContentRevision = contentRevision;
    }

    @Override
    public @NotNull VcsRevisionNumber getRevisionNumber() {
      return myContentRevision.getRevisionNumber();
    }

    @Override
    public @Nullable String loadContent() {
      return loadContentRevision(myProject, myContentRevision);
    }
  }

  private static class HijackedBaseContent implements BaseContent {
    private final @Nullable Project myProject;
    private final @NotNull DiffProvider myDiffProvider;
    private final @NotNull VirtualFile myFile;
    private final @NotNull VcsRevisionNumber myRevision;

    HijackedBaseContent(@Nullable Project project,
                        @NotNull DiffProvider diffProvider,
                        @NotNull VirtualFile file,
                        @NotNull VcsRevisionNumber revision) {
      myProject = project;
      myDiffProvider = diffProvider;
      myFile = file;
      myRevision = revision;
    }

    @Override
    public @NotNull VcsRevisionNumber getRevisionNumber() {
      return myRevision;
    }

    @Override
    public @Nullable String loadContent() {
      ContentRevision contentRevision = myDiffProvider.createFileContent(myRevision, myFile);
      if (contentRevision == null) return null;
      return loadContentRevision(myProject, contentRevision);
    }
  }

  private static @Nullable String loadContentRevision(@Nullable Project project, @NotNull ContentRevision contentRevision) {
    try {
      if (contentRevision instanceof ByteBackedContentRevision) {
        byte[] revisionContent = ((ByteBackedContentRevision)contentRevision).getContentAsBytes();
        FilePath filePath = contentRevision.getFile();

        if (revisionContent != null) {
          return VcsImplUtil.loadTextFromBytes(project, revisionContent, filePath);
        }
        else {
          return null;
        }
      }
      else {
        return contentRevision.getContent();
      }
    }
    catch (VcsException ex) {
      if (LOG.isDebugEnabled()) {
        LOG.debug(ex);
      }
      return null;
    }
  }
}
