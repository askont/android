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
package com.android.tools.idea.tests.gui.framework.fixture.newpsd

import com.android.tools.idea.gradle.structure.configurables.BUILD_VARIANTS_VIEW
import com.android.tools.idea.tests.gui.framework.fixture.IdeFrameFixture
import java.awt.Container

class BuildVariantsPerspectiveConfigurableFixture(
  ideFrameFixture: IdeFrameFixture,
  container: Container
) : BasePerspectiveConfigurableFixture(ideFrameFixture, container) {

  fun selectBuildTypesTab(): BuildTypesFixture =
    selectTab("Build Types") { BuildTypesFixture(ideFrameFixture, it) }

  fun selectProductFlavorsTab(): ProductFlavorsFixture =
    selectTab("Flavors") { ProductFlavorsFixture(ideFrameFixture, it) }
}

fun ProjectStructureDialogFixture.selectBuildVariantsConfigurable(): BuildVariantsPerspectiveConfigurableFixture {
  selectConfigurable("Build Variants")
  return BuildVariantsPerspectiveConfigurableFixture(
      ideFrameFixture,
      findConfigurable(BUILD_VARIANTS_VIEW))
}
