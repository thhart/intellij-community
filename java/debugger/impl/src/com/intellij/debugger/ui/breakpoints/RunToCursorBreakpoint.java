// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.debugger.ui.breakpoints;

import com.intellij.debugger.JavaDebuggerBundle;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.RequestHint;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XLineBreakpointType;
import com.sun.jdi.event.LocatableEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Eugene Zhuravlev
 */
public class RunToCursorBreakpoint extends SyntheticLineBreakpoint implements SteppingBreakpoint {
  private final boolean myRestoreBreakpoints;
  protected final @NotNull SourcePosition myCustomPosition;

  protected RunToCursorBreakpoint(@NotNull Project project, @NotNull SourcePosition pos, boolean restoreBreakpoints) {
    super(project);
    myCustomPosition = pos;
    myRestoreBreakpoints = restoreBreakpoints;
  }

  @Override
  public @NotNull SourcePosition getSourcePosition() {
    return myCustomPosition;
  }

  @Override
  public int getLineIndex() {
    return myCustomPosition.getLine();
  }

  @Override
  public String getFileName() {
    return myCustomPosition.getFile().getName();
  }

  @Override
  protected @Nullable VirtualFile getVirtualFile() {
    return myCustomPosition.getFile().getVirtualFile();
  }

  @Override
  public boolean isRestoreBreakpoints() {
    return myRestoreBreakpoints;
  }

  @Override
  public String getEventMessage(LocatableEvent event) {
    return JavaDebuggerBundle.message("status.stopped.at.cursor");
  }

  @Override
  protected @Nullable JavaLineBreakpointType getXBreakpointType() {
    SourcePosition position = getSourcePosition();
    VirtualFile file = position.getFile().getVirtualFile();
    int line = position.getLine();
    for (XLineBreakpointType<?> type : XDebuggerUtil.getInstance().getLineBreakpointTypes()) {
      if (type instanceof JavaLineBreakpointType && type.canPutAt(file, line, myProject)) {
        return ((JavaLineBreakpointType)type);
      }
    }
    return null;
  }

  protected static @Nullable RunToCursorBreakpoint create(@NotNull Project project,
                                                          @NotNull XSourcePosition position,
                                                          boolean restoreBreakpoints) {
    PsiFile psiFile = PsiManager.getInstance(project).findFile(position.getFile());
    if (psiFile == null) {
      return null;
    }
    return new RunToCursorBreakpoint(project, SourcePosition.createFromOffset(psiFile, position.getOffset()), restoreBreakpoints);
  }

  @Override
  public void setRequestHint(RequestHint hint) {
  }

  @Override
  public boolean track() {
    return false;
  }
}
