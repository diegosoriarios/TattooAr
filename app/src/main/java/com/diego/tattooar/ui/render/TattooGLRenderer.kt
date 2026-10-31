package com.diego.tattooar.ui.render

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class TattooGLRenderer : GLSurfaceView.Renderer {

    private var programHandle = 0
    private var positionHandle = 0
    private var texCoordHandle = 0
    private var mvpMatrixHandle = 0
    private var textureHandle = 0
    private var opacityHandle = 0
    private var inkFadeHandle = 0
    private var isMirroredHandle = 0

    private var textureId = 0
    private var pendingBitmap: Bitmap? = null
    private var isTextureLoaded = false

    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)

    var transformState = TattooTransformState()

    // Quad Vertices [-1, 1]
    private val quadCoords = floatArrayOf(
        -1.0f,  1.0f, 0.0f,
        -1.0f, -1.0f, 0.0f,
         1.0f,  1.0f, 0.0f,
         1.0f, -1.0f, 0.0f
    )

    private val texCoords = floatArrayOf(
        0.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 0.0f,
        1.0f, 1.0f
    )

    private val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(quadCoords.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(quadCoords)
            position(0)
        }

    private val texCoordBuffer: FloatBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(texCoords)
            position(0)
        }

    fun updateTattooBitmap(bitmap: Bitmap) {
        pendingBitmap = bitmap
        isTextureLoaded = false
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, ShaderCode.VERTEX_SHADER)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, ShaderCode.FRAGMENT_SHADER)

        programHandle = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }

        positionHandle = GLES20.glGetAttribLocation(programHandle, "aPosition")
        texCoordHandle = GLES20.glGetAttribLocation(programHandle, "aTexCoord")
        mvpMatrixHandle = GLES20.glGetUniformLocation(programHandle, "uMVPMatrix")
        textureHandle = GLES20.glGetUniformLocation(programHandle, "uTattooTexture")
        opacityHandle = GLES20.glGetUniformLocation(programHandle, "uOpacity")
        inkFadeHandle = GLES20.glGetUniformLocation(programHandle, "uInkFade")
        isMirroredHandle = GLES20.glGetUniformLocation(programHandle, "uIsMirrored")

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.toFloat()
        Matrix.orthoM(projectionMatrix, 0, -aspect, aspect, -1f, 1f, -1f, 1f)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        val bitmap = pendingBitmap
        if (bitmap != null && !isTextureLoaded) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            isTextureLoaded = true
        }

        if (!isTextureLoaded) return

        GLES20.glUseProgram(programHandle)

        // Set Model Transformations
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, transformState.effectiveTranslationX, transformState.effectiveTranslationY, 0f)
        Matrix.rotateM(modelMatrix, 0, transformState.effectiveRotationAngle, 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, transformState.effectiveScale, transformState.effectiveScale, 1f)

        // MVP Matrix
        val mvMatrix = FloatArray(16)
        Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvMatrix, 0)

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniform1f(opacityHandle, transformState.opacity * transformState.trackingAlpha)
        GLES20.glUniform1f(inkFadeHandle, transformState.inkFade)
        GLES20.glUniform1f(isMirroredHandle, if (transformState.isMirrored) 1.0f else 0.0f)

        // Pass Vertices
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)

        // Pass Texture Coords
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 8, texCoordBuffer)

        // Bind Texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(textureHandle, 0)

        // Draw Quad
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }
}
