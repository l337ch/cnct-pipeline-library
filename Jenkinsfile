#!/usr/bin/env groovy

node {
	def workingDir = "${pwd()}@script"
	echo "${workingDir}@script"
	load "${workingDir}/src/net/zonarsystems/pipeline/PipelineHelpers.groovy"
	load "${workingDir}/src/net/zonarsystems/pipeline/applicationPipelineFile.groovy"
/*
	File applicationPipelineFile = new File("${workingDir}/src/net/zonarsystems/pipeline/applicationPipelineFile.groovy");
	Class ApplicationPipelineClass = new GroovyClassLoader(getClass().getClassLoader()).parseClass(sourceFile);
	echo ApplicationPipelineClass.getName()
*/	
	//net.zonarsystems.pipeline.ApplicationPipeline applicationPipeline = (net.zonarsystems.pipeline.ApplicationPipeline) ApplicationPipelineClass.newInstance(steps, 'pipelinelibrary', this);
	
	import net.zonarsystems.pipeline.ApplicationPipeline
	
	//applicationPipeline = new ApplicationPipeline(steps, 'pipelinelibrary', this)
	//applicationPipeline.init()
	//applicationPipeline.pipelineRun()
}