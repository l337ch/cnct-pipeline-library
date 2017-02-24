#!/usr/bin/env groovy

def workingDir = pwd()
echo "${workingDir}"
File sourceFile = new File("${workingDir}/src/net/zonarsystems/pipeline/ApplicationPipeline.groovy");
Class ApplicationPipelineClass = new GroovyClassLoader(getClass().getClassLoader()).parseClass(sourceFile);
echo ApplicationPipelineClass.getName()

//net.zonarsystems.pipeline.ApplicationPipeline applicationPipeline = (net.zonarsystems.pipeline.ApplicationPipeline) ApplicationPipelineClass.newInstance(steps, 'pipelinelibrary', this);

//import net.zonarsystems.pipeline.ApplicationPipeline

//applicationPipeline = new ApplicationPipeline(steps, 'pipelinelibrary', this)
//applicationPipeline.init()
//applicationPipeline.pipelineRun()