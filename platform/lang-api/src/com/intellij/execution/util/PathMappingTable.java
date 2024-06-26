/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.execution.util;

import com.intellij.execution.ExecutionBundle;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.PathMappingSettings;
import com.intellij.util.PathMappingSettings.PathMapping;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;

final class PathMappingTable extends ListTableWithButtons<PathMapping> {
  PathMappingTable() {
    getTableView().getEmptyText().setText(ExecutionBundle.message("empty.text.no.mappings"));
  }

  @Override
  protected ListTableModel<PathMapping> createListModel() {
    ColumnInfo<PathMapping, @NlsContexts.ListItem String> local = 
      new ElementsColumnInfoBase<>(ExecutionBundle.message("path.mapping.column.path.local")) {
      @Override
      public @NlsSafe String valueOf(PathMapping pathMapping) {
        return pathMapping.getLocalRoot();
      }

      @Override
      public boolean isCellEditable(PathMapping pathMapping) {
        return canDeleteElement(pathMapping);
      }

      @Override
      public void setValue(PathMapping pathMapping, String s) {
        if (s.equals(valueOf(pathMapping))) {
          return;
        }
        pathMapping.setLocalRoot(s);
        setModified();
      }

      @Override
      protected String getDescription(PathMapping pathMapping) {
        return valueOf(pathMapping);
      }
    };

    ColumnInfo<PathMapping, @NlsContexts.ListItem String> remote = 
      new ElementsColumnInfoBase<>(ExecutionBundle.message("path.mapping.column.path.remote")) {
      @Override
      public @NlsSafe String valueOf(PathMapping pathMapping) {
        return pathMapping.getRemoteRoot();
      }

      @Override
      public boolean isCellEditable(PathMapping pathMapping) {
        return canDeleteElement(pathMapping);
      }

      @Override
      public void setValue(PathMapping pathMapping, String s) {
        if (s.equals(valueOf(pathMapping))) {
          return;
        }
        pathMapping.setRemoteRoot(s);
        setModified();
      }

      @Override
      protected String getDescription(PathMapping pathMapping) {
        return valueOf(pathMapping);
      }
    };

    return new ListTableModel<>(local, remote);
  }


  public PathMappingSettings getPathMappingSettings() {
    return new PathMappingSettings(getElements());
  }

  @Override
  protected PathMapping createElement() {
    return new PathMapping();
  }

  @Override
  protected boolean isEmpty(PathMapping element) {
    return StringUtil.isEmpty(element.getLocalRoot()) && StringUtil.isEmpty(element.getRemoteRoot());
  }

  @Override
  protected PathMapping cloneElement(PathMapping envVariable) {
    return envVariable.clone();
  }

  @Override
  protected boolean canDeleteElement(PathMapping selection) {
    return true;
  }
}
