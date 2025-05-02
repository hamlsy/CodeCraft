package com.github.hamlsy.codecraft

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.util.Random
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.swing.Timer
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.awt.Color

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener

@Service
class CodeCraftService(private val project: Project) {

    // 블록 이미지들을 저장할 맵
    val blockImages = mutableMapOf<String, BufferedImage>()

    // 활성화된 블록 애니메이션 목록
    private val _activeBlocks = mutableListOf<BlockAnimation>()

    // 설정값
    var enableAnimation = true
    var enableSound = true
    var enableShake = true
    var shakeDuration = 100 // 밀리초
    var shakeIntensity = 2 // 픽셀
    var blockFallSpeed = 2.0 // 블록 떨어지는 속도 기본값
    var blockRotationSpeed = 0.01 // 블록 회전 속도 기본값
    var tntProbability = 0.1 // TNT 블록이 나타날 확률

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
        val editorFactory = EditorFactory.getInstance()
        editorFactory.eventMulticaster.addDocumentListener(createDocumentListener(), project)

        // 애니메이션 타이머 시작
        animationTimer.start()
        
        println("CodeCraft plugin initialized!")
    }
    
    private fun loadBlockImages() {
        // Load only dirt block
        try {
            // Try multiple paths to find the image
            val paths = listOf(
                "/images/new_dirt.png",
                "images/dirt.png",
                "/dirt.png",
                "dirt.png",
                "../resources/images/dirt.png",
                "../../resources/images/dirt.png"
            )
            
            var loaded = false
            
            for (path in paths) {
                try {
                    val imageUrl = javaClass.getResource(path)
                    if (imageUrl != null) {
                        val image = ImageIO.read(imageUrl)
                        if (image != null) {
                            blockImages["dirt"] = image
                            println("Successfully loaded dirt.png from: $path")
                            println("Image size: ${image.width}x${image.height}")
                            loaded = true
                            break
                        } else {
                            println("Failed to read image from: $path (ImageIO.read returned null)")
                        }
                    } else {
                        println("Resource not found: $path")
                    }
                } catch (e: Exception) {
                    println("Error loading image from $path: ${e.message}")
                }
            }
            
            // If all paths failed, create a fallback image
            if (!loaded) {
                println("Creating fallback image for dirt block")
                val fallbackImage = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
                val g = fallbackImage.createGraphics()
                g.color = Color(139, 69, 19)  // Brown color
                g.fillRect(0, 0, 16, 16)
                g.color = Color.BLACK
                g.drawRect(0, 0, 15, 15)
                g.dispose()
                blockImages["dirt"] = fallbackImage
            }
            
            // Debug output
            println("Loaded block images: ${blockImages.keys.joinToString()}")
        } catch (e: Exception) {
            println("Error loading block images: ${e.message}")
            e.printStackTrace()
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
            println("Error loading sounds: ${e.message}")
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
                    if (enableAnimation) createBlockAnimation(event, false)
                } else if (newFragment.length < oldFragment.length) {
                    // 삭제 발생
                    if (enableSound) playDeleteSound()
                    if (enableAnimation) createBlockAnimation(event, true)
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

    private fun createBlockAnimation(event: DocumentEvent, isDelete: Boolean) {
        // Get current active editor and cursor position
        val editor = EditorFactory.getInstance().getEditors(event.document, project).firstOrNull()
        if (editor != null) {
            // Calculate cursor position
            val offset = event.offset
            val point = editor.offsetToXY(offset)
            
            println("Editor size: ${editor.component.width}x${editor.component.height}")
            println("Creating block at cursor position: x=${point.x}, y=${point.y}")

            // Get dirt block image
            val blockImage = blockImages["dirt"]
            if (blockImage != null) {
                println("Block image size: ${blockImage.width}x${blockImage.height}")
                
                val block = BlockAnimation(
                    blockType = "dirt",
                    image = blockImage,
                    startX = point.x,
                    startY = point.y,
                    isTNT = false,
                    isDelete = isDelete
                )

                // Set initial velocity
                block.velocityY = 0.5  // Slow falling speed
                
                // Different initial velocity for delete effect
                if (isDelete) {
                    block.velocityY = -1.0  // Jump up effect
                    block.velocityX = random.nextFloat().toDouble() * 2 - 1  // Random horizontal movement
                }

                // Add to active blocks list
                _activeBlocks.add(block)
                println("Block added. Total active blocks: ${_activeBlocks.size}")
            } else {
                println("Error: dirt block image is null")
            }
        } else {
            println("Error: Could not find editor for document")
        }
    }

    private fun selectRandomBlockType(): String {
        // 항상 dirt 블록만 반환
        return "dirt"
    }

    private fun shakeEditor(type: ShakeType) {
        // 현재 활성 에디터 가져오기
        val editors = EditorFactory.getInstance().allEditors
        for (editor in editors) {
            if (editor.project == project) {
                val shakeEffect = EditorShakeEffect(editor)
                val intensity = when (type) {
                    ShakeType.TYPE -> shakeIntensity
                    ShakeType.DELETE -> shakeIntensity + 1
                    ShakeType.EXPLOSION -> shakeIntensity * 2
                }
                shakeEffect.startShake(shakeDuration, intensity)
            }
        }
    }

    fun testEffects() {
        // 현재 활성 에디터 가져오기
        val editor = EditorFactory.getInstance().allEditors.firstOrNull { it.project == project }
        if (editor != null) {
            // 테스트용 블록 생성
            val centerX = editor.component.width / 2
            val centerY = editor.component.height / 2

            // 여러 종류의 블록 생성
            val blockTypes = blockImages.keys.filter { 
                it != "tnt" && it != "explosion" && it != "explosion_small" 
            }.toList()
            for (i in 0 until 5) {
                val blockType = blockTypes[random.nextInt(blockTypes.size)]
                val blockImage = blockImages[blockType]
                if (blockImage != null) {
                    val offsetX = random.nextInt(200) - 100
                    val offsetY = random.nextInt(100) - 50
                    
                    val block = BlockAnimation(
                        blockType = blockType,
                        image = blockImage,
                        startX = centerX + offsetX,
                        startY = centerY + offsetY,
                        isTNT = blockType == "tnt"
                    )
                    
                    // 랜덤한 초기 속도 설정
                    block.velocityY = random.nextFloat().toDouble() * -10
                    block.velocityX = random.nextFloat().toDouble() * 6 - 3
                    
                    _activeBlocks.add(block)
                }
            }
            
            // TNT 블록 하나 추가
            val tntImage = blockImages["tnt"]
            if (tntImage != null) {
                val block = BlockAnimation(
                    blockType = "tnt",
                    image = tntImage,
                    startX = centerX,
                    startY = centerY,
                    isTNT = true
                )
                _activeBlocks.add(block)
            }
            
            // 흔들림 효과 테스트
            if (enableShake) {
                shakeEditor(ShakeType.EXPLOSION)
            }
            
            // 폭발 소리 테스트
            if (enableSound) {
                playExplosionSound()
            }
        }
    }

    private fun updateAnimations() {
        // 모든 활성 블록 애니메이션 업데이트
        val iterator = _activeBlocks.iterator()
        var blockCount = 0
        
        while (iterator.hasNext()) {
            val block = iterator.next()
            blockCount++

            // 애니메이션 업데이트
            block.update()
            
            // 화면 밖으로 나갔으면 제거
            if (block.y > 2000 || block.y < -500 || block.x < -500 || block.x > 2500) {
                iterator.remove()
                println("Block removed: out of screen")
                continue
            }
        }

        // 에디터 리페인트 요청
        EditorFactory.getInstance().allEditors.forEach { 
            it.component.repaint()
        }
    }
    
    private fun createExplosionParticles(x: Int, y: Int) {
        // 폭발 이미지 추가
        val explosionImg = blockImages["explosion"]
        if (explosionImg != null) {
            val explosion = BlockAnimation(
                blockType = "explosion",
                image = explosionImg,
                startX = x,
                startY = y,
                isTNT = false
            )
            explosion.lifespan = 30  // 폭발 효과는 짧게 유지
            _activeBlocks.add(explosion)
        }
        
        // 작은 파편들 생성
        val blockTypes = blockImages.keys.filter { 
            it != "tnt" && it != "explosion" && it != "explosion_small" 
        }.toList()
        
        for (i in 0 until 8) {
            val blockType = blockTypes[random.nextInt(blockTypes.size)]
            val blockImage = blockImages[blockType]
            if (blockImage != null) {
                val particle = BlockAnimation(
                    blockType = blockType,
                    image = blockImage,
                    startX = x + random.nextInt(40) - 20,
                    startY = y + random.nextInt(40) - 20,
                    isTNT = false
                )
                
                // 파편은 작게 표시
                particle.scale = 0.5
                
                // 랜덤한 초기 속도 설정
                particle.velocityX = random.nextFloat().toDouble() * 10 - 5
                particle.velocityY = random.nextFloat().toDouble() * -10 - 2
                particle.rotationSpeed = random.nextFloat().toDouble() * 0.1
                
                _activeBlocks.add(particle)
            }
        }
    }
    
    fun saveSettings() {
        val settingsState = CodeCraftSettingsState.getInstance()
        settingsState.state.enableAnimation = enableAnimation
        settingsState.state.enableSound = enableSound
        settingsState.state.enableShake = enableShake
        settingsState.state.shakeIntensity = shakeIntensity
        settingsState.state.blockFallSpeed = blockFallSpeed
        settingsState.state.blockRotationSpeed = blockRotationSpeed
        settingsState.state.tntProbability = tntProbability
    }
    
    fun loadSettings() {
        val settingsState = CodeCraftSettingsState.getInstance()
        enableAnimation = settingsState.state.enableAnimation
        enableSound = settingsState.state.enableSound
        enableShake = settingsState.state.enableShake
        shakeIntensity = settingsState.state.shakeIntensity
        blockFallSpeed = settingsState.state.blockFallSpeed
        blockRotationSpeed = settingsState.state.blockRotationSpeed
        tntProbability = settingsState.state.tntProbability
    }
    
    fun cleanup() {
        // 애니메이션 타이머 중지
        animationTimer.stop()
        
        // 사운드 리소스 해제
        typeSound?.close()
        deleteSound?.close()
        explosionSound?.close()
        
        // 활성 블록 목록 비우기
        _activeBlocks.clear()
        
        // 설정 저장
        saveSettings()
    }
    
    fun getActiveBlocks(): List<BlockAnimation> {
        return _activeBlocks.toList()
    }
    
    companion object {
        @JvmStatic
        fun getInstance(project: Project): CodeCraftService {
            return project.service<CodeCraftService>()
        }
    }

    // 블록 애니메이션 클래스
    inner class BlockAnimation(
        val blockType: String,
        val image: BufferedImage,
        startX: Int,
        startY: Int,
        val isTNT: Boolean,
        val isDelete: Boolean = false
    ) {
        // 위치 및 속도
        var x: Int = startX
        var y: Int = startY
        var velocityX: Double = 0.0
        var velocityY: Double = 0.0
        
        // 회전 및 크기
        var rotation: Double = 0.0
        var rotationSpeed: Double = 0.01
        var scale: Double = 1.0
        
        // 투명도 및 수명
        var alpha: Float = 1.0f
        var lifespan: Int = 100
        
        // TNT 폭발 상태
        var isExploded: Boolean = false
        
        // 업데이트 함수
        fun update() {
            // 위치 업데이트
            x += velocityX.toInt()
            y += velocityY.toInt()
            
            // 중력 적용 (천천히 가속)
            velocityY += 0.05
            
            // 회전 업데이트
            rotation += rotationSpeed
            
            // 수명 감소
            if (lifespan > 0) {
                lifespan--
                // 수명이 다 되어가면 투명해지기
                if (lifespan < 30) {
                    alpha = lifespan / 30.0f
                }
            }
        }
    }

    enum class ShakeType {
        TYPE, DELETE, EXPLOSION
    }
}