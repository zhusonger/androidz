package cn.com.lasong.zapp.service.muxer.render

import android.graphics.Bitmap
import android.opengl.Matrix
import cn.com.lasong.media.gles.MEGLHelper
import cn.com.lasong.utils.ILog
import cn.com.lasong.zapp.service.RecordService

/**
 * Author: song.zhu
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/7/26
 * Description:
 * 视频渲染
 */
class VideoRender(glVersion: Int = 3) {

    // gl_Position 为OpenGL坐标系
    // uMVPMatrix 是 投影矩阵(projection matrix) * 视图矩阵(view matrix) * 模型矩阵(model/object matrix)
    // gl_Position = projection * view * model * vec4(position)
    // 顺序很重要
    // 模型矩阵 作用是对物体本身的描述, 用来做旋转、平移、缩放
    // 视图矩阵 作用是从多角度观察物体, 物体不用动, 从物体动改为观察者动, 例如相机
    // 投影矩阵 作用是把四维物体投影到显示屏幕上展示, 正交(大小一致)与透视(有远近大小)投影, 就是平行光与散射光
    //          右手坐标系
    //            y
    //            |
    //            |
    //            |- - - - - x
    //           /
    //          /
    //         z
    //          左手坐标系
    //            y
    //            |
    //            |
    // x - - - - -|
    //           /
    //          /
    //         z
    companion object {
        // 投影矩阵
        val projectionMatrix: FloatArray = FloatArray(16)

        // 视图矩阵
        val viewMatrix: FloatArray = FloatArray(16)

        // 模型矩阵
        val modelMatrix: FloatArray = FloatArray(16)

        // 旋转矩阵
        val rotateMatrix: FloatArray = FloatArray(16)

        // 组合后的矩阵
        val mvpMatrix: FloatArray = FloatArray(16)

        // 使所有矩阵设置为默认的单元矩阵
        private fun identityMatrix() {
            Matrix.setIdentityM(projectionMatrix, 0)
            Matrix.setIdentityM(viewMatrix, 0)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.setIdentityM(rotateMatrix, 0)
            Matrix.setIdentityM(mvpMatrix, 0)
        }

        // (-1,1) 0		(1,1) 3
        //	 ____________
        //  |			 |
        //	|			 |
        //	|____________|
        //(-1, -1)	1	(1, -1) 2
        // opengl世界坐标
        val worldPosition = floatArrayOf(
            -1.0f, 1.0f,  // 0
            -1.0f, -1.0f, // 1
            1.0f, -1.0f,  // 2
            1.0f, 1.0f,   // 3
        )

        // (0,1) 0		(1,1) 3
        //	 ____________
        //  |			 |
        //	|			 |
        //	|____________|
        //(0, 0) 1	(1, 0) 2
        // OpenGL默认纹理坐标
        val texturePosition = floatArrayOf(
            0.0f, 1.0f,  // 0
            0.0f, 0.0f,  // 1
            1.0f, 0.0f,  // 2
            1.0f, 1.0f,  // 3
        )

        // (0,0) 0		(1,0) 3
        //	 ____________
        //  |			 |
        //	|			 |
        //	|____________|
        //(0, 1) 1	(1, 1) 2
        // Android纹理坐标
        // 因为Android坐标系是一个左手坐标系, OpenGL纹理是右手坐标系
        // 所以需要上下翻转
        private val androidPosition = floatArrayOf(
            0.0f, 0.0f,  // 0
            0.0f, 1.0f,  // 1
            1.0f, 1.0f,  // 2
            1.0f, 0.0f,  // 3
        )

        private val indexArray = shortArrayOf(0, 1, 2, 0, 2, 3)

        init {
            identityMatrix()
        }
    }

    private var render: GLESRender = if (glVersion == 3) {
        GLES2Render()
    } else {
        GLES2Render()
    }

    private fun init(): Boolean {
        val ret = render.init()
        render.initBuffer(
            mvpMatrix,
            Pair(worldPosition, 2),
            Pair(texturePosition, 2),
            Pair(androidPosition, 2),
            indexArray
        )
        return ret
    }

    fun doRender(oesTexture: Int, oesMatrix: FloatArray) {
        if (isMatrix) {
            ILog.d(RecordService.TAG, "doFrame update matrix")
            Matrix.setIdentityM(mvpMatrix, 0)
            // gl_Position = projection * view * model * rotate * vec4(position)
            // 1. model * rotate 对物体进行旋转, 进行模型转换, 从模型坐标转换到世界坐标
            Matrix.multiplyMM(
                mvpMatrix, 0,
                modelMatrix, 0,
                rotateMatrix, 0
            )

            // 2. view * model * rotate 进行视图转换, 从世界坐标转换到视图(观察eye)坐标
            var temp = mvpMatrix.copyOf()
            Matrix.multiplyMM(
                mvpMatrix, 0,
                viewMatrix, 0,
                temp, 0
            )

            // 3. projection * view * model * rotate 进行投影转换, 从视图坐标转换到投影(裁剪)坐标
            temp = mvpMatrix.copyOf()
            Matrix.multiplyMM(
                mvpMatrix, 0,
                projectionMatrix, 0,
                temp, 0
            )
            isMatrix = false
            // 4. 更新变化矩阵
            render.updateMVPMatrix(mvpMatrix)
        }
        render.doFrame(renderWidth, renderHeight, oesTexture, oesMatrix)
    }

    fun release() {
        render.release()
        identityMatrix()
    }

    // 是否需要更新矩阵
    private var isMatrix = true

    // 水印
    private var isWaterMark = false
    private var waterMarkWidth = 0
    private var waterMarkHeight = 0

    // 渲染内容
    private var renderWidth = 0
    private var renderHeight = 0

    /**
     * 初始化渲染类
     * @param renderWidth 渲染内容宽
     * @param renderHeight 渲染内容高
     * @param matrix 真实画面的宽高与渲染画面的宽高投影的矩阵
     */
    fun init(renderWidth: Int, renderHeight: Int, matrix: FloatArray): Boolean {
        this.renderWidth = renderWidth
        this.renderHeight = renderHeight
        // 模型/旋转矩阵默认单位矩阵
        Matrix.setIdentityM(rotateMatrix, 0)
        Matrix.setIdentityM(modelMatrix, 0)
        // Set the camera position (View matrix)
        // 视图矩阵, 右手坐标系, 从eye坐标到center坐标为观察的方向, up为摄像头的方向
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        // 设置投影矩阵, 高度撑满, 宽度自适应内容的比例
        // near/far就是投影的近/远平面, 方向是视图矩阵定义的方向(eye到center的方向)
        System.arraycopy(matrix, 0, projectionMatrix, 0, matrix.size)
        return init()
    }

    /**
     * 更新投影矩阵
     */
    fun updateProjection(matrix: FloatArray) {
        System.arraycopy(matrix, 0, projectionMatrix, 0, matrix.size)
        isMatrix = true
    }

    /**
     * 更新水印和宽高
     * @param waterTexture 水印纹理
     * @param watermark 水印图片
     */
    fun updateWaterMark(waterTexture: Int, watermark: Bitmap?) {
        isWaterMark = null != watermark
        if (null != watermark) {
            MEGLHelper.glBindTexture2D(waterTexture, watermark)
            waterMarkWidth = watermark.width
            waterMarkHeight = watermark.height
        }
    }

    /**
     * 处理旋转矩阵来保证屏幕转向时, 渲染时把画面转回到原来的位置
     * @param target 目标屏幕的方向
     * @param current 当前屏幕的方向
     */
    fun rotate(target: Int, current: Int) {
        // target: R_0(0) current: R_90(1) => 屏幕顺时针绕Z轴旋转90度 => 需要逆时针绕Z轴旋转90度
        // target: R_0(0) current: R_270(3) => 屏幕顺时针绕Z轴旋转90度 => 需要逆时针绕Z轴旋转270度
        // target: R_90(1) current: R_0(0) => 屏幕逆时针绕Z轴旋转90度 => 需要顺时针绕Z轴旋转90度
        // 总结: 从target怎么转过去, 逆向转过来相同度数
        // degree 正为顺时针, 负数为逆时针
        val degree = -(current - target) * 90f
        Matrix.setRotateM(rotateMatrix, 0, degree, 0f, 0f, 1f)
        isMatrix = true
    }

}