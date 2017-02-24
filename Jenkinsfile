#!/usr/bin/env groovy

node {
	def workingDir = "${pwd()}@script"
	echo "${workingDir}@script"
	pipelineHelper = load "${workingDir}/src/net/zonarsystems/pipeline/PipelineHelpers.groovy"
	applicationPipe = load "${workingDir}/src/net/zonarsystems/pipeline/applicationPipelineFile.groovy"

	File pipelineHelperFile = new File("${workingDir}/src/net/zonarsystems/pipeline/PipelineHelpers.groovy");
	File applicationPipelineFile = new File("${workingDir}/src/net/zonarsystems/pipeline/ApplicationPipeline.groovy");
	
	Class net.zonarsystems.pipeline.PipelineHelpers = new GroovyClassLoader(getClass().getClassLoader()).parseClass(pipelineHelperFile);
	Class net.zonarsystems.pipeline.ApplicationPipeline = new GroovyClassLoader(getClass().getClassLoader()).parseClass(applicationPipelineFile);

	
	//net.zonarsystems.pipeline.ApplicationPipeline applicationPipeline = (net.zonarsystems.pipeline.ApplicationPipeline) ApplicationPipelineClass.newInstance(steps, 'pipelinelibrary', this);
	
	//import net.zonarsystems.pipeline.ApplicationPipeline
	
	//applicationPipeline = new ApplicationPipeline(steps, 'pipelinelibrary', this)
	//applicationPipeline.init()
	//applicationPipeline.pipelineRun()
}