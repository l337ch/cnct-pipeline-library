#!/usr/bin/env groovy

def runTest(applicationPipeline) {
	applicationPipeline.init()
	applicationPipeline.pipelineRun()
}

node {
 	if (env.CHANGE_ID) {
		print "testing library PR ${env.CHANGE_ID}"
		def lib = library("pipeline@refs/remotes/origin/pr/${env.CHANGE_ID}")
		
		applicationPipeline = lib.net.zonarsystems.pipeline.ApplicationPipeline.new(
		  steps, 
		  'pipelinelibrary', 
		  this
		)
		runTest(applicationPipeline)
	} else {
		print "testing library on master"
		def lib = library("pipeline")
		applicationPipeline = lib.net.zonarsystems.pipeline.ApplicationPipeline.new.ApplicationPipeline(
		  steps, 
		  'pipelinelibrary', 
		  this
		)
		runTest(applicationPipeline)
	}
}