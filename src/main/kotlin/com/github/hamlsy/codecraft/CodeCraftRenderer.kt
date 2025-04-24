package com.github.hamlsy.codecraft


import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.markup.HighlighterLayer
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import javax.swing.JComponent
import javax.swing.JLayeredPane

/**
 * 에디터에 블록 애니메이션을 렌더링하는 컴포넌트
 */
class CodeCraftEditorComponent(private val editor: Editor) : JComponent() {

    private val service = editor.project?.getService(CodeCraftService::class.java)

    init {
        isOpaque = false
    }

    override fun paint(g: Graphics) {
        super.paint(g)

        if (service == null || !service.enableAnimation) return

        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // 모든 활성 블록 렌더링
        service.activeBlocks.forEach { block ->
            // 현재 블록의 변환 상태 저장
            val oldTransform = g2d.transform

            // 블록 회전 적용
            val centerX = block.x + block.image.width / 2
            val centerY = block.y + block.image.height / 2

            val transform = AffineTransform()
            transform.translate(centerX.toDouble(), centerY.toDouble())
            transform.rotate(block.rotation.toDouble())
            transform.translate((-block.image.width / 2).toDouble(), (-block.image.height / 2).toDouble())

            g2d.transform = transform

            // 블록 이미지 그리기
            g2d.drawImage(block.image, 0, 0, null)

            // TNT가 폭발 상태면 폭발 효과 그리기
            if (block.isTNT && block.isExploded) {
                // 폭발 이미지나 이펙트 그리기
                service.blockImages["explosion"]?.let { explosionImg ->
                    g2d.drawImage(
                        explosionImg,
                        -explosionImg.width / 4,
                        -explosionImg.height / 4,
                        explosionImg.width * 2,
                        explosionImg.height * 2,
                        null
                    )
                }
            }

            // 변환 상태 복원
            g2d.transform = oldTransform
        }
    }

    companion object {
        // 에디터 팩토리 리스너 생성
        fun createEditorFactoryListener(): EditorFactoryListener {
            return object : EditorFactoryListener {
                override fun editorCreated(event: EditorFactoryEvent) {
                    val editor = event.editor
                    val component = CodeCraftEditorComponent(editor)

                    // 에디터의 레이어드 패널에 컴포넌트 추가
                    val layeredPane = editor.component.rootPane?.layeredPane
                    layeredPane?.add(component, JLayeredPane.POPUP_LAYER)

                    // 컴포넌트 위치와 크기 설정
                    component.setBounds(0, 0, layeredPane?.width ?: 0, layeredPane?.height ?: 0)

                    // 레이어드 패널 크기 변경 리스너 추가
                    layeredPane?.addComponentListener(object : java.awt.event.ComponentAdapter() {
                        override fun componentResized(e: java.awt.event.ComponentEvent) {
                            component.setBounds(0, 0, layeredPane.width, layeredPane.height)
                        }
                    })
                }

                override fun editorReleased(event: EditorFactoryEvent) {
                    // 에디터가 해제될 때 클린업 코드
                }
            }
        }
    }
}

/**
 * 에디터 흔들림 효과 구현
 */
class EditorShakeEffect(private val editor: Editor) {
    private val originalPosition = java.awt.Point()
    private var shakeTimer: javax.swing.Timer? = null

    init {
        // 에디터 컴포넌트의 원래 위치 저장
        originalPosition.x = editor.component.x
        originalPosition.y = editor.component.y
    }

    fun startShake(duration: Int, intensity: Int) {
        // 이미 실행 중인 타이머 중지
        shakeTimer?.stop()

        // 원래 위치로 복원
        resetPosition()

        val random = java.util.Random()
        var remainingTime = duration

        // 새 타이머 시작
        shakeTimer = javax.swing.Timer(16) { // 약 60 FPS
            if (remainingTime <= 0) {
                resetPosition()
                shakeTimer?.stop()
                return@Timer
            }

            // 랜덤한 오프셋 적용
            val offsetX = random.nextInt(intensity * 2) - intensity
            val offsetY = random.nextInt(intensity * 2) - intensity

            editor.component.setLocation(originalPosition.x + offsetX, originalPosition.y + offsetY)

            remainingTime -= 16
        }

        shakeTimer?.start()
    }

    private fun resetPosition() {
        editor.component.setLocation(originalPosition.x, originalPosition.y)
    }
}