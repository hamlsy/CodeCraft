package com.github.hamlsy.codecraft

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.startup.StartupActivity

/**
 * 프로젝트가 열릴 때 CodeCraft 서비스를 초기화하는 클래스
 */
class CodeCraftStartupActivity : StartupActivity, ProjectManagerListener {
    override fun runActivity(project: Project) {
        // 프로젝트 서비스 초기화
        val service = project.getService(CodeCraftService::class.java)
        
        // 설정 로드
        service.loadSettings()
        
        // 서비스 초기화
        service.initialize()
        
        // 에디터 팩토리에 리스너 등록
        val editorFactory = EditorFactory.getInstance()
        editorFactory.addEditorFactoryListener(
            CodeCraftEditorComponent.createEditorFactoryListener(),
            project
        )
    }
    
    override fun projectOpened(project: Project) {
        // 프로젝트가 열릴 때 실행
        runActivity(project)
    }
    
    override fun projectClosed(project: Project) {
        // 프로젝트가 닫힐 때 정리 작업
        val service = project.getService(CodeCraftService::class.java)
        service.cleanup()
    }
}