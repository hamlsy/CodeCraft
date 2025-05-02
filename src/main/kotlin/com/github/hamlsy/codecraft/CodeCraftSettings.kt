package com.github.hamlsy.codecraft


import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBSlider
import com.intellij.util.ui.FormBuilder
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import java.awt.FlowLayout
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JTextField
import javax.swing.border.TitledBorder
import javax.swing.border.EmptyBorder

/**
 * CodeCraft 설정 UI 구현
 */
class CodeCraftConfigurable : Configurable {
    private var mainPanel: JPanel? = null
    private var enableAnimationCheckbox: JBCheckBox? = null
    private var enableSoundCheckbox: JBCheckBox? = null
    private var enableShakeCheckbox: JBCheckBox? = null
    private var shakeIntensitySlider: JBSlider? = null
    private var blockFallSpeedSlider: JBSlider? = null
    private var blockRotationSpeedSlider: JBSlider? = null
    private var tntProbabilitySlider: JBSlider? = null
    private var testButton: JButton? = null

    override fun getDisplayName(): String = "CodeCraft Settings"

    override fun createComponent(): JComponent {
        // 서비스 인스턴스 가져오기
        val service = CodeCraftSettingsState.getInstance()

        // UI 컴포넌트 생성
        enableAnimationCheckbox = JBCheckBox("Enable Block Animations", service.state.enableAnimation)
        enableSoundCheckbox = JBCheckBox("Enable Sound Effects", service.state.enableSound)
        enableShakeCheckbox = JBCheckBox("Enable Shake Effects", service.state.enableShake)

        // 슬라이더 생성
        shakeIntensitySlider = JBSlider(1, 10, service.state.shakeIntensity).apply {
            majorTickSpacing = 1
            paintTicks = true
            paintLabels = true
        }

        blockFallSpeedSlider = JBSlider(1, 10, (service.state.blockFallSpeed * 2).toInt()).apply {
            majorTickSpacing = 1
            paintTicks = true
            paintLabels = true
        }

        blockRotationSpeedSlider = JBSlider(1, 10, (service.state.blockRotationSpeed * 100).toInt()).apply {
            majorTickSpacing = 1
            paintTicks = true
            paintLabels = true
        }

        tntProbabilitySlider = JBSlider(1, 20, (service.state.tntProbability * 100).toInt()).apply {
            majorTickSpacing = 5
            paintTicks = true
            paintLabels = true
        }

        testButton = JButton("Test Effects").apply {
            addActionListener {
                // 현재 프로젝트 가져오기
                val projects = com.intellij.openapi.project.ProjectManager.getInstance().openProjects
                if (projects.isNotEmpty()) {
                    val project = projects[0]
                    val projectService = project.getService(CodeCraftService::class.java)
                    
                    // 현재 설정값 적용
                    projectService.enableAnimation = enableAnimationCheckbox?.isSelected ?: true
                    projectService.enableSound = enableSoundCheckbox?.isSelected ?: true
                    projectService.enableShake = enableShakeCheckbox?.isSelected ?: true
                    projectService.shakeIntensity = shakeIntensitySlider?.value ?: 2
                    projectService.blockFallSpeed = (blockFallSpeedSlider?.value ?: 4).toDouble() / 2.0
                    projectService.blockRotationSpeed = (blockRotationSpeedSlider?.value ?: 1).toDouble() / 100.0
                    projectService.tntProbability = (tntProbabilitySlider?.value ?: 10).toDouble() / 100.0
                    
                    // 테스트 효과 실행
                    projectService.testEffects()
                }
            }
        }

        // 기본 설정 패널
        val basicSettingsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(enableAnimationCheckbox!!)
            add(enableSoundCheckbox!!)
            add(enableShakeCheckbox!!)
            border = TitledBorder("Basic Settings")
        }

        // 효과 설정 패널
        val effectSettingsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(JBLabel("Shake Intensity:"))
            add(shakeIntensitySlider!!)
            add(JBLabel("Block Fall Speed:"))
            add(blockFallSpeedSlider!!)
            add(JBLabel("Block Rotation Speed:"))
            add(blockRotationSpeedSlider!!)
            add(JBLabel("TNT Probability (%):"))
            add(tntProbabilitySlider!!)
            border = TitledBorder("Effect Settings")
        }

        // 테스트 버튼 패널
        val testButtonPanel = JPanel(FlowLayout(FlowLayout.CENTER)).apply {
            add(testButton!!)
        }

        // 메인 패널 구성
        mainPanel = JPanel(BorderLayout()).apply {
            add(basicSettingsPanel, BorderLayout.NORTH)
            add(effectSettingsPanel, BorderLayout.CENTER)
            add(testButtonPanel, BorderLayout.SOUTH)
            border = EmptyBorder(10, 10, 10, 10)
        }

        return mainPanel!!
    }

    override fun isModified(): Boolean {
        val service = CodeCraftSettingsState.getInstance()

        return enableAnimationCheckbox?.isSelected != service.state.enableAnimation ||
                enableSoundCheckbox?.isSelected != service.state.enableSound ||
                enableShakeCheckbox?.isSelected != service.state.enableShake ||
                shakeIntensitySlider?.value != service.state.shakeIntensity ||
                blockFallSpeedSlider?.value != (service.state.blockFallSpeed * 2).toInt() ||
                blockRotationSpeedSlider?.value != (service.state.blockRotationSpeed * 100).toInt() ||
                tntProbabilitySlider?.value != (service.state.tntProbability * 100).toInt()
    }

    override fun apply() {
        val service = CodeCraftSettingsState.getInstance()

        service.state.enableAnimation = enableAnimationCheckbox?.isSelected ?: true
        service.state.enableSound = enableSoundCheckbox?.isSelected ?: true
        service.state.enableShake = enableShakeCheckbox?.isSelected ?: true
        service.state.shakeIntensity = shakeIntensitySlider?.value ?: 2
        service.state.blockFallSpeed = (blockFallSpeedSlider?.value ?: 4) / 2.0
        service.state.blockRotationSpeed = (blockRotationSpeedSlider?.value ?: 1) / 100.0
        service.state.tntProbability = (tntProbabilitySlider?.value ?: 10) / 100.0

        // 모든 열린 프로젝트에 설정 적용
        val projects = com.intellij.openapi.project.ProjectManager.getInstance().openProjects
        for (project in projects) {
            val projectService = project.getService(CodeCraftService::class.java)
            projectService.loadSettings()
        }
    }

    override fun reset() {
        val service = CodeCraftSettingsState.getInstance()

        enableAnimationCheckbox?.isSelected = service.state.enableAnimation
        enableSoundCheckbox?.isSelected = service.state.enableSound
        enableShakeCheckbox?.isSelected = service.state.enableShake
        shakeIntensitySlider?.value = service.state.shakeIntensity
        blockFallSpeedSlider?.value = (service.state.blockFallSpeed * 2).toInt()
        blockRotationSpeedSlider?.value = (service.state.blockRotationSpeed * 100).toInt()
        tntProbabilitySlider?.value = (service.state.tntProbability * 100).toInt()
    }
}

/**
 * 설정값을 영구 저장하기 위한 PersistentStateComponent 구현
 */
@State(
    name = "CodeCraftSettings",
    storages = [Storage("CodeCraftSettings.xml")]
)
class CodeCraftSettingsState : PersistentStateComponent<CodeCraftSettingsState.State> {

    class State {
        var enableAnimation: Boolean = true
        var enableSound: Boolean = true
        var enableShake: Boolean = true
        var shakeIntensity: Int = 2
        var blockFallSpeed: Double = 2.0
        var blockRotationSpeed: Double = 0.01
        var tntProbability: Double = 0.1
    }

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }
    
    companion object {
        @JvmStatic
        fun getInstance(): CodeCraftSettingsState {
            return service()
        }
    }
}