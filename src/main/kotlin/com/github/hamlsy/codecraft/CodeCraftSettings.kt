package com.github.hamlsy.codecraft


import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBSlider
import com.intellij.util.ui.FormBuilder
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * CodeCraft 설정 UI 구현
 */
class CodeCraftConfigurable : Configurable {
    private var mainPanel: JPanel? = null
    private var enableAnimationCheckbox: JBCheckBox? = null
    private var enableSoundCheckbox: JBCheckBox? = null
    private var enableShakeCheckbox: JBCheckBox? = null
    private var shakeIntensitySlider: JBSlider? = null
    private var testButton: JButton? = null

    override fun getDisplayName(): String = "CodeCraft Settings"

    override fun createComponent(): JComponent {
        // 서비스 인스턴스 가져오기
        val service = CodeCraftService.getInstance();

        // UI 컴포넌트 생성
        enableAnimationCheckbox = JBCheckBox("Enable Block Animations", service.enableAnimation)
        enableSoundCheckbox = JBCheckBox("Enable Sound Effects", service.enableSound)
        enableShakeCheckbox = JBCheckBox("Enable Shake Effects", service.enableShake)

        shakeIntensitySlider = JBSlider(1, 10, service.shakeIntensity).apply {
            majorTickSpacing = 1
            paintTicks = true
            paintLabels = true
        }

        testButton = JButton("Test Effects").apply {
            addActionListener {
                service.testEffects()
            }
        }

        // 폼 빌더를 사용하여 UI 구성
        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(JBLabel("CodeCraft Plugin Settings"))
            .addVerticalGap(10)
            .addComponent(enableAnimationCheckbox!!)
            .addComponent(enableSoundCheckbox!!)
            .addComponent(enableShakeCheckbox!!)
            .addSeparator()
            .addLabeledComponent("Shake Intensity:", shakeIntensitySlider!!)
            .addVerticalGap(10)
            .addComponent(testButton!!)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        return mainPanel!!
    }

    override fun isModified(): Boolean {
        val service = CodeCraftService.getInstance()

        return enableAnimationCheckbox?.isSelected != service.enableAnimation ||
                enableSoundCheckbox?.isSelected != service.enableSound ||
                enableShakeCheckbox?.isSelected != service.enableShake ||
                shakeIntensitySlider?.value != service.shakeIntensity
    }

    override fun apply() {
        val service = CodeCraftService.getInstance()

        service.enableAnimation = enableAnimationCheckbox?.isSelected ?: true
        service.enableSound = enableSoundCheckbox?.isSelected ?: true
        service.enableShake = enableShakeCheckbox?.isSelected ?: true
        service.shakeIntensity = shakeIntensitySlider?.value ?: 2

        // 설정 저장
        service.saveSettings()
    }

    override fun reset() {
        val service = CodeCraftService.getInstance()

        enableAnimationCheckbox?.isSelected = service.enableAnimation
        enableSoundCheckbox?.isSelected = service.enableSound
        enableShakeCheckbox?.isSelected = service.enableShake
        shakeIntensitySlider?.value = service.shakeIntensity
    }
}

/**
 * 설정값을 영구 저장하기 위한 PersistentStateComponent 구현
 */
@com.intellij.openapi.components.State(
    name = "CodeCraftSettings",
    storages = [com.intellij.openapi.components.Storage("CodeCraftSettings.xml")]
)
class CodeCraftSettingsState : com.intellij.openapi.components.PersistentStateComponent<CodeCraftSettingsState.State> {

    class State {
        var enableAnimation: Boolean = true
        var enableSound: Boolean = true
        var enableShake: Boolean = true
        var shakeIntensity: Int = 2
    }

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }
}