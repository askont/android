/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.tools.idea.layoutinspector.model

import com.android.tools.idea.layoutinspector.LayoutInspector
import com.android.tools.idea.layoutinspector.SkiaParser
import com.android.tools.idea.layoutinspector.common.StringTable
import com.android.tools.idea.layoutinspector.resource.ResourceLookup
import com.android.tools.idea.layoutinspector.transport.DefaultInspectorClient
import com.android.tools.idea.layoutinspector.transport.InspectorClient
import com.android.tools.layoutinspector.proto.LayoutInspectorProto
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.ui.UIUtil
import java.awt.Image
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

private val LOAD_TIMEOUT = TimeUnit.SECONDS.toMillis(20)

object ComponentTreeLoader : TreeLoader {
  override fun loadComponentTree(
    maybeEvent: Any?, resourceLookup: ResourceLookup, client: InspectorClient
  ): ViewNode? {
    val event = maybeEvent as? LayoutInspectorProto.LayoutInspectorEvent ?: return null
    return ComponentTreeLoaderImpl(event.tree, resourceLookup).loadComponentTree(client)
  }
}

private class ComponentTreeLoaderImpl(
  private val tree: LayoutInspectorProto.ComponentTreeEvent, private val resourceLookup: ResourceLookup?
) {
  private val loadStartTime = AtomicLong(-1)
  private val stringTable = StringTable(tree.stringList)

  fun loadComponentTree(client: InspectorClient): ViewNode? {
    val time = System.currentTimeMillis()
    if (time - loadStartTime.get() < LOAD_TIMEOUT) {
      return null
    }
    return try {
      val rootView = loadRootView()
      val bytes = client.getPayload(tree.payloadId)
      var viewRoot: InspectorView? = null
      if (bytes.isNotEmpty()) {
        try {
          viewRoot = SkiaParser.getViewTree(bytes)
          if (viewRoot != null && viewRoot.id.isEmpty()) {
            // We were unable to parse the skia image. Allow the user to interact with the component tree.
            viewRoot = null
          }
          (client as? DefaultInspectorClient)?.logInitialRender(viewRoot != null)
        }
        catch (ex: Exception) {
          Logger.getInstance(LayoutInspector::class.java).warn(ex)
        }
      }
      if (viewRoot != null) {
        val imageLoader = ComponentImageLoader(rootView, viewRoot)
        imageLoader.loadImages()
      }
      rootView
    }
    finally {
      loadStartTime.set(0)
    }
  }

  private fun loadRootView(): ViewNode {
    resourceLookup?.updateConfiguration(tree.resources, stringTable)
    return loadView(tree.root, null)
  }

  private fun loadView(view: LayoutInspectorProto.View, parent: ViewNode?): ViewNode {
    val qualifiedName = "${stringTable[view.packageName]}.${stringTable[view.className]}"
    val viewId = stringTable[view.viewId]
    val textValue = stringTable[view.textValue]
    val layout = stringTable[view.layout]
    val x = view.x + (parent?.let { it.x - it.scrollX } ?: 0)
    val y = view.y + (parent?.let { it.y - it.scrollY } ?: 0)
    val node = ViewNode(view.drawId, qualifiedName, layout, x, y, view.scrollX, view.scrollY,
                                                                     view.width, view.height, viewId, textValue)
    view.subViewList.map { loadView(it, node) }.forEach {
      node.children.add(it)
      it.parent = node
    }
    return node
  }

  private class ComponentImageLoader(root: ViewNode, viewRoot: InspectorView) {
    private val nodeMap = root.flatten().associateBy { it.drawId }
    private val viewMap = viewRoot.flatten().associateBy { it.id.toLong() }

    fun loadImages() {
      for ((drawId, node) in nodeMap) {
        val view = viewMap[drawId] ?: continue
        node.imageBottom = view.image
        addChildNodeImages(node, view)
      }
    }

    private fun addChildNodeImages(node: ViewNode, view: InspectorView) {
      var beforeChildren = true
      for (child in view.children.values) {
        val isChildNode = view.id != child.id && nodeMap.containsKey(child.id.toLong())
        when {
          isChildNode -> beforeChildren = false
          beforeChildren -> node.imageBottom = combine(node.imageBottom, child, node.bounds)
          else -> node.imageTop = combine(node.imageTop, child, node.bounds)
        }
        if (!isChildNode) {
          // Some Skia views are several levels deep:
          addChildNodeImages(node, child)
        }
      }
    }

    private fun combine(image: Image?,
                        view: InspectorView,
                        bounds: Rectangle): Image? {
      if (view.image == null) {
        return image
      }
      @Suppress("UndesirableClassUsage")
      val result = image ?: BufferedImage(bounds.width, bounds.height, (view.image as BufferedImage).type)
      // Combine the images...
      val g = result.graphics
      UIUtil.drawImage(g, view.image!!, view.x - bounds.x, view.y - bounds.y, null)
      g.dispose()
      return result
    }
  }
}