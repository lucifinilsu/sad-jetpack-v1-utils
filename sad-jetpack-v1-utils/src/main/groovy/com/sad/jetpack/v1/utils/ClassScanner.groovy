package com.sad.jetpack.v1.utils

import com.android.build.api.transform.Format
import com.android.build.api.transform.TransformInvocation
import groovy.io.FileType
import javassist.ClassPath
import javassist.ClassPool
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

import java.lang.reflect.Constructor

public class ClassScanner {
    private Project project
    private ClassPool classPool
    private TransformInvocation transformInvocation
    private OnFileScannedCallback scannedCallback
    private ClassScanner(Project project){
        this.project=project
    }
    static ClassScanner newInstance(Project project){
        return new ClassScanner(project)
    }
    ClassScanner classPool(ClassPool classPool){
        this.classPool=classPool
        return this
    }

    ClassScanner transformInvocation(TransformInvocation transformInvocation){
        this.transformInvocation=transformInvocation
        return this
    }
    ClassScanner scannedCallback(OnFileScannedCallback scannedCallback){
        this.scannedCallback=scannedCallback
        return this
    }

    void into(ITarget target){
        doScan(target)
    }

    private void doScan2(ITarget target){

    }

    private void doScan(ITarget target){
        def classPath = []
        try {

            Class jarClassPathClazz = Class.forName("javassist.JarClassPath")
            Constructor constructor = jarClassPathClazz.getDeclaredConstructor(String.class)
            constructor.setAccessible(true)
            //ClassScanResult classScanResult=new ClassScanResult();
            transformInvocation.inputs.each { input ->

                def subProjectInputs = []

                input.jarInputs.each { jarInput ->
                    project.logger.error(">>>>jar input=   " + jarInput.file.getAbsolutePath())
                    ClassPath clazzPath = (ClassPath) constructor.newInstance(jarInput.file.absolutePath)
                    classPath.add(clazzPath)
                    classPool.appendClassPath(clazzPath)

                    def jarName = jarInput.name
                    if (jarName.endsWith(".jar")) {
                        jarName = jarName.substring(0, jarName.length() - 4)
                    }
                    //project.logger.error("jar name   " + jarName)
                    if (jarName.startsWith(":")) {
                        // project.logger.error("jar name startsWith冒号   " + jarName)
                        //handle it later, after classpath set
                        subProjectInputs.add(jarInput)
                    } else {
                        def dest = transformInvocation.outputProvider.getContentLocation(jarName,
                                jarInput.contentTypes, jarInput.scopes, Format.JAR)
                        // project.logger.error("jar output path:" + dest.getAbsolutePath())
                        FileUtils.copyFile(jarInput.file, dest)
                    }
                }

                // Handle library project jar here
                subProjectInputs.each { jarInput ->

                    def jarName = jarInput.name
                    if (jarName.endsWith(".jar")) {
                        jarName = jarName.substring(0, jarName.length() - 4)
                    }

                    if (jarName.startsWith(":")) {
                        // sub project
                        File unzipDir = new File(
                                jarInput.file.getParent(),
                                jarName.replace(":", "") + "_unzip")
                        if (unzipDir.exists()) {
                            unzipDir.delete()
                        }
                        unzipDir.mkdirs()
                        Decompression.uncompress(jarInput.file, unzipDir)

                        File repackageFolder = new File(
                                jarInput.file.getParent(),
                                jarName.replace(":", "") + "_repackage"
                        )

                        FileUtils.copyDirectory(unzipDir, repackageFolder)
                        //.copyDirectory(unzipDir, repackageFolder)

                        unzipDir.eachFileRecurse(FileType.FILES) { File it ->
                            //checkAndTransformClass(classPool, it, repackageFolder)
                            if (scannedCallback!=null){
                                scannedCallback.onScanned(classPool,it,repackageFolder)
                            }
                        }

                        // re-package the folder to jar
                        def dest = transformInvocation.outputProvider.getContentLocation(
                                jarName, jarInput.contentTypes, jarInput.scopes, Format.JAR)

                        Compressor zc = new Compressor(dest.getAbsolutePath())
                        zc.compress(repackageFolder.getAbsolutePath())
                    }
                }

                input.directoryInputs.each { dirInput ->
                    def outDir = transformInvocation.outputProvider.getContentLocation(dirInput.name, dirInput.contentTypes, dirInput.scopes, Format.DIRECTORY)
                    classPool.appendClassPath(dirInput.file.absolutePath)
                    // dirInput.file is like "build/intermediates/classes/debug"
                    int pathBitLen = dirInput.file.toString().length()

                    def callback = { File it ->
                        def path = "${it.toString().substring(pathBitLen)}"
                        if (it.isDirectory()) {
                            new File(outDir, path).mkdirs()
                        } else {
                            boolean handled = false;//checkAndTransformClass(classPool, it, outDir)
                            if (scannedCallback!=null){
                                handled=scannedCallback.onScanned(classPool, it, outDir)
                            }
                            if (!handled) {
                                // copy the file to output location
                                new File(outDir, path).bytes = it.bytes
                            }
                        }
                    }
                    if (dirInput.changedFiles == null || dirInput.changedFiles.isEmpty()) {
                        dirInput.file.traverse(callback)
                    } else {
                        dirInput.changedFiles.keySet().each(callback)
                    }
                }
            }
            if (target!=null){
                target.onScannedCompleted(classPool)
            }

        }
        catch (Exception e){
            e.printStackTrace()
        }
        finally {
            classPath.each { it ->
                classPool.removeClassPath(it)
            }
        }
    }


    /*static void scan(Project project,ClassPool classPool ,TransformInvocation transformInvocation, OnFileScannedCallback scannedCallback){

    }*/
    
    interface OnFileScannedCallback{
        boolean onScanned(ClassPool classPool,File scannedFile, File dest);
    }

    interface ITarget {
        void onScannedCompleted(ClassPool classPool);
    }
}
