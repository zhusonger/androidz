package cn.com.lasong.inject;


import com.android.build.gradle.BaseExtension;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionContainer;
import org.jetbrains.annotations.NotNull;

import cn.com.lasong.utils.PluginHelper;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020/8/13
 * Description: 注入插件
 */
public class InjectPlugin implements Plugin<Project> {

    public static final String EXTENSION_NAME = "allInjects";

    @Override
    public void apply(@NotNull Project project) {
        // 只处理应用和库模块
        if (!PluginHelper.isAppOrLibrary(project)) {
            return;
        }

        // 拼装项目名称
        StringBuilder builder = new StringBuilder();
        builder.append(":").append(project.getName());
        Project parent = project.getParent();
        while (PluginHelper.isAppOrLibrary(parent)) {
            builder.insert(0, parent.getName()+":");
            parent = parent.getParent();
        }
        String name = builder.toString();

        // 开始注册
        PluginHelper.println(name, "InjectPlugin Start");
        ExtensionContainer extensions = project.getExtensions();

        setupExtension(project);

        BaseExtension android = extensions.findByType(BaseExtension.class);
        assert android != null;
        //注册task任务
        android.registerTransform(new InjectTransform(project, PluginHelper.isApplication(project),
                name));
        PluginHelper.println(name, "InjectPlugin Finish");
    }

    /**
     * 设置插件配置
     * @param project
     */
    private void setupExtension(Project project) {
        ExtensionContainer extensions = project.getExtensions();

        // 调用构造方法, 第一个是project
        extensions.create(EXTENSION_NAME, InjectExtension.class, project);
    }
}
