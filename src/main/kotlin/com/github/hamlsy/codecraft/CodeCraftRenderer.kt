package com.github.hamlsy.codecraft


import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.AlphaComposite
import java.awt.geom.AffineTransform
import javax.swing.JComponent
import javax.swing.JLayeredPane

/**
 * 에디터에 블록 애니메이션을 렌더링하는 컴포넌트
 */
class CodeCraftEditorComponent(private val editor: Editor) : JComponent() {

    private val project = editor.project
    private val service = project?.getService(CodeCraftService::class.java)

    init {
        // Set component properties
        isOpaque = false
        
        // Set preferred size to match editor
        preferredSize = editor.component.size
        
        // Debug output
        println("CodeCraftEditorComponent initialized for editor: ${editor.document.text.length} chars")
        println("Editor component size: ${editor.component.width}x${editor.component.height}")
    }

    override fun paint(g: Graphics) {
        super.paint(g)
        
        // Get graphics object
        val g2d = g as Graphics2D

        // Set rendering hints for better quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)

        // Get service and active blocks
        val service = project?.getService(CodeCraftService::class.java)
        if (service == null || !service.enableAnimation) {
            println("Animation disabled or service is null")
            return
        }
        
        val blocks = service.getActiveBlocks()
        if (blocks.isNotEmpty()) {
            println("Rendering ${blocks.size} blocks")
        }

        // Draw each block
        for (block in blocks) {
            try {
                // Save current transform and composite
                val oldTransform = g2d.transform
                val oldComposite = g2d.composite
                
                // Set alpha for transparency
                g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, block.alpha)
                
                // Calculate center point for rotation
                val centerX = block.x
                val centerY = block.y
                
                // Apply transformations (translate, rotate, scale)
                g2d.translate(centerX, centerY)
                g2d.rotate(block.rotation)
                g2d.scale(3.0, 3.0)  // Scale by 3x to make blocks visible
                
                // Draw the image
                g2d.drawImage(block.image, -block.image.width / 2, -block.image.height / 2, null)
                
                // Draw red border for debugging
                g2d.color = java.awt.Color.RED
                g2d.drawRect(-block.image.width / 2, -block.image.height / 2, block.image.width, block.image.height)
                
                // Restore original transform and composite
                g2d.transform = oldTransform
                g2d.composite = oldComposite
                
                // Draw coordinates for debugging (in screen space)
                g2d.color = java.awt.Color.RED
                g2d.drawString("x=${block.x}, y=${block.y}", block.x, block.y - 5)
            } catch (e: Exception) {
                println("Error rendering block: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    companion object {
        // 에디터 팩토리 리스너 생성
        fun createEditorFactoryListener(): EditorFactoryListener {
            return object : EditorFactoryListener {
                override fun editorCreated(event: EditorFactoryEvent) {
                    val editor = event.editor
                    val project = editor.project ?: return
                    
                    // Create component
                    val component = CodeCraftEditorComponent(editor)
                    
                    // Add to editor's layered pane
                    val layeredPane = editor.contentComponent.parent as? JLayeredPane
                    layeredPane?.add(component, JLayeredPane.POPUP_LAYER)
                    
                    // Set bounds to match editor
                    component.bounds = editor.component.bounds
                    
                    println("Added CodeCraftEditorComponent to editor layered pane")
                    
                    // Add component resize listener
                    editor.component.addComponentListener(object : java.awt.event.ComponentAdapter() {
                        override fun componentResized(e: java.awt.event.ComponentEvent) {
                            component.bounds = editor.component.bounds
                            component.preferredSize = editor.component.size
                            component.revalidate()
                            component.repaint()
                        }
                    })
                }

                override fun editorReleased(event: EditorFactoryEvent) {
                    // Clean up when editor is released
                    val editor = event.editor
                    val layeredPane = editor.contentComponent.parent as? JLayeredPane
                    
                    // Find and remove our component
                    layeredPane?.components?.forEach { component ->
                        if (component is CodeCraftEditorComponent) {
                            layeredPane.remove(component)
                            println("Removed CodeCraftEditorComponent from editor layered pane")
                        }
                    }
                    
                    // Repaint to ensure clean removal
                    layeredPane?.repaint()
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

            // 랜덤한 오프셋 적용 (진폭은 시간이 지남에 따라 감소)
            val factor = remainingTime.toFloat() / duration
            val offsetX = (random.nextInt(intensity * 2) - intensity) * factor
            val offsetY = (random.nextInt(intensity * 2) - intensity) * factor

            editor.component.setLocation(originalPosition.x + offsetX.toInt(), originalPosition.y + offsetY.toInt())

            remainingTime -= 16
        }

        shakeTimer?.start()
    }

    private fun resetPosition() {
        editor.component.setLocation(originalPosition.x, originalPosition.y)
    }
}