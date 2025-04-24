package com.github.hamlsy.codecraft

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import java.util.Random
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.swing.Timer
import java.awt.Point
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

/**
 * CodeCraft 플러그인 메인 클래스
 * 에디터에 문서 리스너를 등록하고 애니메이션 효과를 관리합니다.
 */
class CodeCraftStartupActivity : StartupActivity {
    override fun runActivity(project: Project) {
        // 플러그인 서비스 초기화
        val service = project.getService(CodeCraftService::class.java)
        service.initialize()
    }
}