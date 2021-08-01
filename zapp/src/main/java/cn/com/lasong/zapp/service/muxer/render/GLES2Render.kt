package cn.com.lasong.zapp.service.muxer.render

import android.opengl.GLES11Ext
import android.opengl.GLES20
import cn.com.lasong.media.gles.MEGLHelper
import java.nio.FloatBuffer

/**
 * Author: song.zhu
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/7/26
 * Description:
 */
class GLES2Render : GLESRender {

    private companion object {
        // OpenGL ES2.0 绘制程序
        const val vertex: String =
                    "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 aPosition;  \n" +
                    "uniform mat4 uTextureMatrix;\n" +
                    "attribute vec4 aInTexturePosition;  \n" + // 因为取到的oes矩阵是4x4的, 所以要转换就需要列行数对应
                    "varying vec2 vTexturePosition;\n" + // 我们需要的是二维的坐标, 所以转换后取二维坐标即可, 减少数据量
                    "void main(){               \n" +
                    " vTexturePosition = (uTextureMatrix * aInTexturePosition).xy;\n" +
                    " gl_Position = uMVPMatrix * aPosition; \n" +
                    "}"
        const val fragment: String =
                    "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTexturePosition;\n" +
                    "uniform samplerExternalOES sTextureOES;\n" +
                    "uniform sampler2D sTexture2D;\n" +
                    "uniform bool is2D;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = is2D ? texture2D(sTexture2D, vTexturePosition) " +
                    ": texture2D(sTextureOES, vTexturePosition);\n" +
                    "}\n"
    }

    private var program: Int = 0
    private var aPositionLocation = 0
    private var uTextureMatrixLocation: Int = 0
    private var uMVPMatrixLocation: Int = 0
    private var aInTexturePositionLocation: Int = 0
    private var sTextureOESLocation: Int = 0
    private var sTexture2DLocation: Int = 0
    private var is2DLocation: Int = 0

    private lateinit var mvpMatrixBuffer: FloatBuffer
    private var worldPositionBuffer = 0
    private var texturePositionBuffer = 0
    private var androidPositionBuffer = 0
    private var worldPositionSize = 0
    private var texturePositionSize = 0
    private var androidPositionSize = 0
    private var indexArrayElementBuffer = 0
    private var indexArraySize = 0
    override fun init(): Boolean {
        // 1. 创建program
        program = MEGLHelper.createProgram(vertex, fragment)
        if (program == 0) {
            return false
        }
        // 2. 获取属性位置
        // vertex
        uMVPMatrixLocation = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        aPositionLocation = GLES20.glGetAttribLocation(program, "aPosition")
        uTextureMatrixLocation = GLES20.glGetUniformLocation(program, "uTextureMatrix")
        aInTexturePositionLocation = GLES20.glGetAttribLocation(program, "aInTexturePosition")
        // fragment
        sTextureOESLocation = GLES20.glGetUniformLocation(program, "sTextureOES")
        sTexture2DLocation = GLES20.glGetUniformLocation(program, "sTexture2D")
        is2DLocation = GLES20.glGetUniformLocation(program, "is2D")
        return true
    }

    override fun initBuffer(
        mvpMatrix: FloatArray,
        worldPosition: Pair<FloatArray, Int>,
        texturePosition: Pair<FloatArray, Int>,
        androidPosition: Pair<FloatArray, Int>,
        indexArray: ShortArray
    ) {
        // 1. 生成坐标及索引buffer
        val buffers = IntArray(4)
        GLES20.glGenBuffers(buffers.size, buffers, 0)
        worldPositionBuffer = buffers[0]
        texturePositionBuffer = buffers[1]
        androidPositionBuffer = buffers[2]
        indexArrayElementBuffer = buffers[3]

        worldPositionSize = worldPosition.second
        texturePositionSize = texturePosition.second
        androidPositionSize = androidPosition.second
        indexArraySize = indexArray.size
        // 2. 绑定数据
        MEGLHelper.glBindVertexBufferData(
            worldPositionBuffer,
            MEGLHelper.allocateFloatBuffer(worldPosition.first),
            GLES20.GL_STATIC_DRAW
        )
        MEGLHelper.glBindVertexBufferData(
            texturePositionBuffer,
            MEGLHelper.allocateFloatBuffer(texturePosition.first),
            GLES20.GL_STATIC_DRAW
        )
        MEGLHelper.glBindVertexBufferData(
            androidPositionBuffer,
            MEGLHelper.allocateFloatBuffer(androidPosition.first),
            GLES20.GL_STATIC_DRAW
        )
        MEGLHelper.glBindElementBufferData(
            indexArrayElementBuffer,
            MEGLHelper.allocateShortBuffer(indexArray),
            GLES20.GL_STATIC_DRAW
        )
        // 解除绑定
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
        // 创建mvp buffer
        mvpMatrixBuffer = MEGLHelper.allocateFloatBuffer(mvpMatrix)
    }

    override fun updateMVPMatrix(mvpMatrix: FloatArray) {
        mvpMatrixBuffer.put(mvpMatrix).position(0)
    }

    override fun doFrame(width: Int, height: Int, oesTexture: Int, oesMatrix: FloatArray) {
        if (program == 0) {
            return
        }
        GLES20.glViewport(0, 0, width, height)
        GLES20.glUseProgram(program)

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        // vertex
        // 更新世界坐标
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, worldPositionBuffer)
        GLES20.glEnableVertexAttribArray(aPositionLocation)
        GLES20.glVertexAttribPointer(
            aPositionLocation,
            worldPositionSize,
            GLES20.GL_FLOAT,
            false,
            0,
            0
        )
        // 更新世界坐标矩阵变换
        GLES20.glUniformMatrix4fv(uMVPMatrixLocation, 1, false, mvpMatrixBuffer)
        // 更新纹理变换矩阵
        GLES20.glUniformMatrix4fv(uTextureMatrixLocation, 1, false, oesMatrix, 0)
        // 更新纹理坐标
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, texturePositionBuffer)
        GLES20.glEnableVertexAttribArray(aInTexturePositionLocation)
        GLES20.glVertexAttribPointer(
            aInTexturePositionLocation,
            texturePositionSize,
            GLES20.GL_FLOAT,
            false,
            0,
            0
        )
        // fragment
        // 更新为纹理类型flag, 非2D纹理
        GLES20.glUniform1i(is2DLocation, 0)
        // 更新oes纹理
        GLES20.glUniform1i(sTextureOESLocation, 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTexture)
        // 更新2D纹理, program里有的, 没有也需要设置
        GLES20.glUniform1i(sTexture2DLocation, 1)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexArrayElementBuffer)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexArraySize, GLES20.GL_UNSIGNED_SHORT, 0)

        // 解绑buffer
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    override fun release() {
        MEGLHelper.glDeleteProgram(program)
        MEGLHelper.glDeleteBuffers(intArrayOf(worldPositionBuffer, texturePositionBuffer,
            androidPositionBuffer, indexArrayElementBuffer))
    }
}