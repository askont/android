/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.gradle.project.build.output;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTask;
import com.intellij.openapi.externalSystem.service.execution.DefaultExternalSystemExecutionConsoleManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleConstants;

/**
 * ConsoleManager for Gradle Sync.
 *
 * This is used to ensure that we use our AndroidGradleSyncTextConsoleView so we can correctly surface the errors and warnings from sync.
 */
public class AndroidGradleExecutionConsoleManager extends DefaultExternalSystemExecutionConsoleManager {
  @NotNull
  @Override
  public ProjectSystemId getExternalSystemId() {
    return GradleConstants.SYSTEM_ID;
  }

  @Nullable
  @Override
  public ExecutionConsole attachExecutionConsole(@NotNull Project project,
                                                 @NotNull ExternalSystemTask task,
                                                 @Nullable ExecutionEnvironment env,
                                                 @Nullable ProcessHandler processHandler) {
    return new AndroidGradleSyncTextConsoleView(project);
  }

  @Override
  public boolean isApplicableFor(@NotNull ExternalSystemTask task) {
    return GradleConstants.SYSTEM_ID.equals(task.getId().getProjectSystemId());
  }
}