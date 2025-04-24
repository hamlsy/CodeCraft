package com.github.hamlsy.codecraft

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.util.Random
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.swing.Timer
import java.awt.Point
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener

@Service
class CodeCraftService(private val project: Project) {
    // 블록 이미지들을 저장할 맵
    private val blockImages = mutableMapOf<String, BufferedImage>()

    // 활성화된 블록 애니메이션 목록
    private val activeBlocks = mutableListOf<BlockAnimation>()

    // 설정값
    var enableAnimation = true
    var enableSound = true
    var enableShake = true
    var shakeDuration = 100 // 밀리초
    var shakeIntensity = 2 // 픽셀

    // 소리 클립
    private var typeSound: Clip? = null
    private var deleteSound: Clip? = null
    private var explosionSound: Clip? = null

    // 랜덤 생성기
    private val random = Random()

    // 업데이트 타이머
    private val animationTimer = Timer(16) { updateAnimations() }

    fun initialize() {
        // 이미지 로드
        loadBlockImages()

        // 소리 로드
        loadSounds()

        // 문서 리스너 추가
        val editorFactory = EditorFactory.getInstance().eventMulticaster
        editorFactory.addDocumentListener(createDocumentListener(), project)

        // 애니메이션 타이머 시작
        animationTimer.start()
    }

    private fun loadBlockImages() {
        val blockTypes = listOf("dirt", "stone", "grass", "tnt", "diamond")

        blockTypes.forEach { type ->
            try {
                val imageStream = javaClass.getResourceAsStream("/images/$type.png")
                if (imageStream != null) {
                    blockImages[type] = ImageIO.read(imageStream)
                }
            } catch (e: Exception) {
                // 이미지 로드 실패 로깅
            }
        }
    }

    private fun loadSounds() {
        try {
            // 타이핑 소리 로드
            val typeStream = javaClass.getResourceAsStream("/sounds/type.wav")
            if (typeStream != null) {
                val audioStream = AudioSystem.getAudioInputStream(typeStream)
                typeSound = AudioSystem.getClip()
                typeSound?.open(audioStream)
            }

            // 삭제 소리 로드
            val deleteStream = javaClass.getResourceAsStream("/sounds/delete.wav")
            if (deleteStream != null) {
                val audioStream = AudioSystem.getAudioInputStream(deleteStream)
                deleteSound = AudioSystem.getClip()
                deleteSound?.open(audioStream)
            }

            // 폭발 소리 로드
            val explosionStream = javaClass.getResourceAsStream("/sounds/explosion.wav")
            if (explosionStream != null) {
                val audioStream = AudioSystem.getAudioInputStream(explosionStream)
                explosionSound = AudioSystem.getClip()
                explosionSound?.open(audioStream)
            }
        } catch (e: Exception) {
            // 소리 로드 실패 로깅
        }
    }

    private fun createDocumentListener(): DocumentListener {
        return object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                if (event.isWholeTextReplaced) return

                val document = event.document
                val newFragment = event.newFragment
                val oldFragment = event.oldFragment

                if (newFragment.length > oldFragment.length) {
                    // 타이핑 발생
                    if (enableSound) playTypeSound()
                    if (enableAnimation) createBlockAnimation(event)
                    if (enableShake) shakeEditor(ShakeType.TYPE)
                } else if (newFragment.length < oldFragment.length) {
                    // 삭제 발생
                    if (enableSound) playDeleteSound()
                    if (enableShake) shakeEditor(ShakeType.DELETE)
                }
            }
        }
    }

    private fun playTypeSound() {
        typeSound?.framePosition = 0
        typeSound?.start()
    }

    private fun playDeleteSound() {
        deleteSound?.framePosition = 0
        deleteSound?.start()
    }

    private fun playExplosionSound() {
        explosionSound?.framePosition = 0
        explosionSound?.start()
    }

    private fun createBlockAnimation(event: DocumentEvent) {
        // 현재 활성 에디터와 커서 위치 가져오기
        val editor = EditorFactory.getInstance().editors.firstOrNull { it.document == event.document }
        if (editor != null) {
            // 커서 위치 계산
            val offset = event.offset
            val point = editor.offsetToXY(offset)

            // 랜덤 블록 유형 선택
            val blockType = selectRandomBlockType()

            // 새 블록 애니메이션 생성
            val block = BlockAnimation(
                blockType = blockType,
                image = blockImages[blockType] ?: return,
                startX = point.x,
                startY = point.y + editor.lineHeight,
                isTNT = blockType == "tnt"
            )

            // 활성 블록 목록에 추가
            activeBlocks.add(block)
        }
    }

    private fun selectRandomBlockType(): String {
        val types = blockImages.keys.toList()
        // TNT는 10% 확률로 등장
        return if (random.nextDouble() < 0.1) "tnt" else types[random.nextInt(types.size)]
    }

    private fun shakeEditor(type: ShakeType) {
        // 여기서 에디터 흔들림 효과를 구현합니다.
        // JComponent의 위치를 약간씩 변경하는 방식으로 구현할 수 있습니다.
    }

    private fun updateAnimations() {
        // 모든 활성 블록 애니메이션 업데이트
        val iterator = activeBlocks.iterator()
        while (iterator.hasNext()) {
            val block = iterator.next()

            // 애니메이션 업데이트
            block.update()

            // 화면 밖으로 나갔으면 제거
            if (block.y > 2000) {
                iterator.remove()
            }

            // TNT 블록이 바닥에 가까워지면 폭발 효과
            if (block.isTNT && block.y > 1000 && !block.isExploded) {
                block.isExploded = true
                playExplosionSound()
                // 여기서 폭발 애니메이션을 시작할 수 있습니다
            }
        }

        // 에디터 리페인트 요청
        EditorFactory.getInstance().allEditors.forEach { it.component.repaint() }
    }