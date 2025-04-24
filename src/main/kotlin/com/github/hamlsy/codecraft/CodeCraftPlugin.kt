package com.github.hamlsy.codecraft


import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity


class CodeCraftStartupActivity : StartupActivity {
    override fun runActivity(project: Project) {
        val service = project.getService(CodeCraftService::class.java)
        service.initialize()
    }
}