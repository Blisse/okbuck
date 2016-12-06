package com.uber.okbuck.core.model.java

import com.android.builder.model.LintOptions
import com.uber.okbuck.OkBuckGradlePlugin
import com.uber.okbuck.core.model.base.Scope
import com.uber.okbuck.core.model.base.Target
import com.uber.okbuck.core.model.jvm.JvmTarget
import com.uber.okbuck.core.util.LintUtil
import org.apache.commons.io.IOUtils
import org.gradle.api.JavaVersion
import org.gradle.api.Project

import java.util.jar.JarEntry
import java.util.jar.JarFile

abstract class JavaTarget extends JvmTarget {

    JavaTarget(Project project, String name) {
        super(project, name)
    }

    /**
     * Apt Scope
     */
    Scope getApt() {
        Scope aptScope = new Scope(project, ["apt", "provided", 'compileOnly', "annotationProcessor"])
        aptScope.targetDeps.retainAll(aptScope.targetDeps.findAll { Target target ->
            target.getProp(okbuck.annotationProcessors, null) != null
        })
        return aptScope
    }

    /**
     * Lint Scope
     */
    Scope getLint() {
        File res = null
        Set<File> sourceDirs = []
        List<String> jvmArguments = []
        return new Scope(project, [OkBuckGradlePlugin.BUCK_LINT], sourceDirs, res, jvmArguments,
                LintUtil.getLintDepsCache(project))
    }

    LintOptions getLintOptions() {
        return null
    }

    boolean hasLintRegistry() {
        try {
            return project.jar.manifest.attributes.containsKey("Lint-Registry")
        } catch (Exception ignored) {
            return false
        }
    }

    /**
     * List of annotation processor classes.
     */
    Set<String> getAnnotationProcessors() {
        return (apt.externalDeps.collect { String aptDep ->
            JarFile jar = new JarFile(new File(aptDep))
            jar.entries().findAll { JarEntry entry ->
                (entry.name == "META-INF/services/javax.annotation.processing.Processor")
            }.collect { JarEntry aptEntry ->
                IOUtils.toString(jar.getInputStream(aptEntry))
                        .trim().split("\\n").findAll { String entry ->
                    !entry.startsWith('#') && !entry.trim().empty // filter out comments and empty lines
                }
            }
        } + apt.targetDeps.collect { Target target ->
            (List<String>) target.getProp(okbuck.annotationProcessors, null)
        }.findAll { List<String> processors ->
            processors != null
        }).flatten() as List<String>
    }

    protected static String javaVersion(JavaVersion version) {
        switch (version) {
            case JavaVersion.VERSION_1_6:
                return '6'
            case JavaVersion.VERSION_1_7:
                return '7'
            case JavaVersion.VERSION_1_8:
                return '8'
            case JavaVersion.VERSION_1_9:
                return '9'
            default:
                return '7'
        }
    }

    @Override
    void resolve() {
        super.resolve()
        getApt()
        getLint()
    }
}