package com.intellij.execution.multilaunch.statistics

import com.intellij.execution.impl.statistics.RunConfigurationTypeDefs
import com.intellij.execution.impl.statistics.RunConfigurationTypeUsagesCollector
import com.intellij.internal.statistic.eventLog.events.EventField
import com.intellij.internal.statistic.service.fus.collectors.FeatureUsageCollectorExtension

internal class MultiLaunchConfigurationTypeCollectorExtension : FeatureUsageCollectorExtension {
  override fun getGroupId() = RunConfigurationTypeDefs.TYPE_USAGES_GROUP_ID

  override fun getEventId() = RunConfigurationTypeUsagesCollector.CONFIGURED_IN_PROJECT

  override fun getExtensionFields(): List<EventField<*>> = listOf(
    FusExecutableRows.FIELD,
    MultiLaunchEventFields.ACTIVATE_TOOL_WINDOWS_FIELD)
}