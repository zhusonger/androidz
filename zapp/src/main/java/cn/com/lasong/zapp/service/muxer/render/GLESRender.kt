package cn.com.lasong.zapp.service.muxer.render


/**
 * Author: song.zhu
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/7/26
 * Description:
 * GLES具体实现
 */
interface GLESRender {

    /**
     * 初始化
     * 创建program
     */
    fun init(): Boolean


    fun initBuffer(
        mvpMatrix: FloatArray,
        worldPosition: Pair<FloatArray, Int>,
        texturePosition: Pair<FloatArray, Int>,
        androidPosition: Pair<FloatArray, Int>,
        indexArray: ShortArray
    )

    fun updateMVPMatrix(mvpMatrix: FloatArray)

    /**
     * @param oesTexture 屏幕的oes纹理
     * @param oesMatrix surface纹理矩阵
     */
    fun doFrame(width: Int, height: Int, oesTexture: Int, oesMatrix: FloatArray)

    /**
     * 释放
     * 销毁program
     */
    fun release()
}
