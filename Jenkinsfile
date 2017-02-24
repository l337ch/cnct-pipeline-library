#!/usr/bin/env groovy

def classLoader = BootStrap.class.classLoader
classLoader.rootLoader.addURL(new URL("file:///src"))

import java.util.zip.ZipOutputStream  
import java.util.zip.ZipEntry  
import java.nio.channels.FileChannel  
  
String jarFileName = "pipeline-library.jar"  
String inputDir = "src"  
  
ZipOutputStream zipFile = new ZipOutputStream(new FileOutputStream(jarFileName))  
new File(inputDir).eachFile() { file ->  
zipFile.putNextEntry(new ZipEntry(file.getName()))  
def buffer = new byte[1024]  
file.withInputStream { i ->  
def l = i.read(buffer)  
// check wether the file is empty  
if (l > 0) {  
zipFile.write(buffer, 0, l)  
}  
}  
zipFile.closeEntry()  
}  
zipFile.close()
this.class.classLoader.rootLoader.addURL(
   new URL("file:///"+jarFileName))  

import net.zonarsystems.pipeline.ApplicationPipeline

applicationPipeline = new ApplicationPipeline(steps, 'pipelinelibrary', this)
applicationPipeline.init()
applicationPipeline.pipelineRun()